package com.ssy.ferry.mytrace;

import com.ssy.ferry.core.Constants;
import com.ssy.ferry.core.MethodMonitor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class TraceDataUtils {
    private static final String TAG = "Matrix.TraceDataUtils";

    public interface IStructuredDataFilter {
        boolean isFilter(long during, int fiterCount);

        int getFilterMaxCount();

        void fallback(List<MethodItem> stack, int size);

    }

    public static void structuredDataToStack(long[] buffer, LinkedList<MethodItem> result, boolean isStrict, long endTime) {
        long lastInId = 0L;
        int depth = 0;
        // push  第一个  pop 也是第一个
        LinkedList<Long> rawData = new LinkedList<>();
        boolean isBegin = !isStrict;//如果是严格方式 isbegin 为false  需要下面判断才可以设置为 true
        for (long trueId : buffer) {
            if (0 == trueId) {
                continue;
            }
            //如果是严格模式那么 只有是 I 且 是 METHOD_ID_DISPATCH 方法才可以放到堆栈
            if (isStrict) {
                if (isIn(trueId) && MethodMonitor.METHOD_ID_DISPATCH == getMethodId(trueId)) {
                    isBegin = true;
                }
                if (!isBegin) {
                    FerryLog.d(TAG, "never begin! pass this method[%s]", getMethodId(trueId));
                    continue;
                }
            }

            if (isIn(trueId)) {
                lastInId = getMethodId(trueId);
                //如果是 METHOD_ID_DISPATCH 方法说明是开始的方法 深度置零 重新计数深度
                if (lastInId == MethodMonitor.METHOD_ID_DISPATCH) {
                    depth = 0;
                }
                depth++;
                rawData.push(trueId);
            } else {//如果是 O 出口
                int outMethodId = getMethodId(trueId);
                if (!rawData.isEmpty()) {
                    long in = rawData.pop();//这里是这样的 有出口就有入口 出口对应最近的入口 这样才是一对
                    depth--;
                    int inMethodId;
                    LinkedList<Long> temp = new LinkedList<>();
                    temp.add(in);
                    //当 i o 不成对 也就是methodId 不同的时候，rawData 不断poll 出 i 方法，然后放入 temp 中 深度减一 直到rawdata为空 或者 i o 方法id一致
                    while ((inMethodId = getMethodId(trueId)) != outMethodId && !rawData.isEmpty()) {
                        FerryLog.w(TAG, "pop inMethodId[%s] to continue match outMethodId[%s]", inMethodId, outMethodId);
                        in = rawData.poll();
                        depth--;
                        temp.add(in);
                    }
                    //如果rawdata 拿空了 i 和 o 和不匹配 而且这个方法是 METHOD_ID_DISPATCH
                    if (inMethodId != outMethodId && inMethodId == MethodMonitor.METHOD_ID_DISPATCH) {
                        FerryLog.e(TAG, "inMethodId[%s] !=outMethodId[%s] throw this outMethodId!", inMethodId, outMethodId);
                        rawData.addAll(temp);
                        depth += rawData.size();
                        continue;
                    }

                    long outTime = geteTime(trueId);
                    long inTime = geteTime(in);
                    long durTime = outTime - inTime;
                    if (durTime < 0) {
                        FerryLog.e(TAG, "[structuredDataToStack] trace during invalid:%d", durTime);
                        rawData.clear();
                        result.clear();
                        return;

                    }
                    MethodItem methodItem = new MethodItem(outMethodId, (int) durTime, depth);
                    addMethodItem(result, methodItem);

                } else {
                    FerryLog.w(TAG, "[structuredDataToStack] method[%s] not found in! ", outMethodId);
                }

            }
        }

        while (!rawData.isEmpty() && isStrict) {
            long turnId = rawData.pop();
            int methodId = getMethodId(turnId);
            boolean isIn = isIn(turnId);
            long inTime = geteTime(turnId) + MethodMonitor.diffTime;
            FerryLog.w(TAG, "[structuredDataToStack] has never out method[%s], isIn:%s, inTime:%s, endTime:%s,rawData size:%s",
                    methodId, isIn, inTime, endTime, rawData.size());
            if (!isIn) {
                FerryLog.e(TAG, "[structuredDataToStack] why has out Method[%s]? is wrong! ", methodId);
                continue;
            }
            MethodItem methodItem = new MethodItem(methodId, (int) (endTime - inTime), rawData.size());
            addMethodItem(result, methodItem);
        }
        TreeNode root = new TreeNode(null, null);
        stackToTree(result, root);
        result.clear();
        treeToStack(root, result);
    }

    //把栈中的 变成树
    private static int stackToTree(LinkedList<MethodItem> result, TreeNode root) {
        TreeNode lastNode = null;
        ListIterator<MethodItem> iterator = result.listIterator(0);
        int count = 0;
        while (iterator.hasNext()) {
            TreeNode node = new TreeNode(iterator.next(), lastNode);
            count++;
            if (null == lastNode && node.depth() != 0) {
                FerryLog.e(TAG, "[stackToTree] begin error! why the first node'depth is not 0!");
                return 0;
            }
            int depth = node.depth();
            if (lastNode == null || depth == 0) {
                root.add(node);
            } else if (lastNode.depth() >= depth) {//如果last深度 大于等于 此节点  说明这个节点不是last的子节点 ，last 可能是这个节点子节点
                //那么就遍历last的父节点直到last 小于等于 此节点
                while (lastNode != null && lastNode.depth() > depth) {
                    lastNode = lastNode.father;
                }
                //如果 last 的 父节点不为null 那么就把last的父节点赋值给 这个节点作为它的父节点 然后last的父节点把node节点添加为子节点
                if (lastNode.father != null) {
                    node.father = lastNode.father;
                    lastNode.father.add(node);
                }

            } else if (lastNode.depth() < depth) {//如果 last深度小于 此节点 就说明这个节点是last的子节点 直接添加子节点就好哦啊
                lastNode.add(node);
            }
            lastNode = node;


        }
        return count;
    }

    private static void rechange(TreeNode root) {
        if (root.children.isEmpty()) {
            return;
        }
        TreeNode[] nodes = new TreeNode[root.children.size()];
        root.children.toArray(nodes);
        root.children.clear();
        for (TreeNode node : nodes) {
            root.children.addFirst(node);
            rechange(node);
        }
    }

    //这个是 类似二叉树左右中 添加到 栈中的
    private static void treeToStack(TreeNode root, LinkedList<MethodItem> result) {

        for (int i = 0; i < root.children.size(); i++) {
            TreeNode child = root.children.get(i);
            result.add(child.item);
            if (!child.children.isEmpty()) {
                treeToStack(child, result);
            }
        }
    }

    private static boolean isIn(long trueId) {
        return ((trueId >> 63) & 0x1) == 1;
    }

    private static long geteTime(long trueId) {
        return trueId & 0x7FFFFFFFFFFL;
    }

    private static int getMethodId(long trueId) {
        return (int) ((trueId >> 43) & 0xFFFFFL);
    }

    private static int addMethodItem(LinkedList<MethodItem> resultStack, MethodItem item) {
        MethodItem last = null;
        if (!resultStack.isEmpty()) {
            last = resultStack.peek();
        }
        if (null != last && last.methodId == item.methodId && last.depth == item.depth && 0 != item.depth) {
            item.durTime = item.durTime == Constants.DEFAULT_ANR ? last.durTime : item.durTime;
            last.mergeMore(item.durTime);
            return item.durTime;
        } else {
            resultStack.push(item);
            return item.durTime;
        }
    }

    private static void rechage(TreeNode root) {

    }

    /**
     * it`s the node for the stack tree
     */
    public static final class TreeNode {
        MethodItem item;
        TreeNode father;

        LinkedList<TreeNode> children = new LinkedList<>();

        TreeNode(MethodItem item, TreeNode father) {
            this.item = item;
            this.father = father;
        }

        private int depth() {
            return null == item ? 0 : item.depth;
        }

        private void add(TreeNode root, StringBuilder print) {
            print.append("|* TraceStack: ").append("\n");
            printTree(root, 0, print, "|*        ");

        }

        private void add(TreeNode node) {
            children.addFirst(node);
        }

        private boolean isLeaf() {
            return children.isEmpty();
        }

        private void printTree(TreeNode root, int depth, StringBuilder print, String prefixStr) {
            StringBuilder empty = new StringBuilder(prefixStr);
            for (int i = 0; i <= depth; i++) {
                empty.append("      ");
            }
            for (int i = 0; i < root.depth(); i++) {
                TreeNode node = root.children.get(i);
                print.append(empty.toString()).append(node.item.methodId).append("[").append(node.item.durTime).append("]").append("\n");
                if (!node.children.isEmpty()) {
                    printTree(node, depth + 1, print, prefixStr);
                }
            }
        }


    }

    /**
     * 主要用于去除一些不必要的 MethodItem 比如耗时微乎其微的
     *
     * @param stack
     * @param targetCount
     * @param filter
     */
    public static void trimStack(List<MethodItem> stack, int targetCount, IStructuredDataFilter filter) {
        //如果期望数量为负数直接返回
        if (0 > targetCount) {
            stack.clear();
            return;
        }
        //筛选次数  如果第一次筛选还是达不到预期效果 就要第二次 当然筛选条件也要想要 提高了 具体多少在我们IStructuredDataFilter来实现
        int filterCount = 1;
        int curStackSize = stack.size();
        while (curStackSize > targetCount) {
            //通过迭代器我们拿到最后的元素  栈底的元素  我们根据durTime 会去掉一些元素 具体durTime 我们IStructuredDataFilter来实现
            ListIterator<MethodItem> iterator = stack.listIterator(stack.size());
            while (iterator.hasPrevious()) {
                MethodItem item = iterator.previous();
                if (filter.isFilter(item.durTime, filterCount)) {
                    iterator.remove();
                    curStackSize--;
                    //当数量满足 期望的时候终止
                    if (curStackSize < targetCount) {
                        return;
                    }
                }
            }

            curStackSize = stack.size();
            filterCount++;
            //筛选不能一直筛选下去 必须有最大值 超过这个最大值 那么就说明筛选失败 不能再筛选了
            if (filter.getFilterMaxCount() < filterCount) {
                break;
            }
        }
        int size = stack.size();
        if (size > targetCount) {
            filter.fallback(stack, size);
        }

    }

    public static long stackToString(LinkedList<MethodItem> stack, StringBuilder reportBuilder, StringBuilder logcatBuilder) {
        logcatBuilder.append("|*   TraceStack:").append("\n");
        logcatBuilder.append("|*        [id count cost]").append("\n");
        Iterator<MethodItem> listIterator = stack.iterator();
        long stackCost = 0; // fix cost
        while (listIterator.hasNext()) {
            MethodItem item = listIterator.next();
            reportBuilder.append(item.toString()).append('\n');
            logcatBuilder.append("|*        ").append(item.print()).append('\n');

            if (stackCost < item.durTime) {
                stackCost = item.durTime;
            }
        }
        return stackCost;
    }
}
