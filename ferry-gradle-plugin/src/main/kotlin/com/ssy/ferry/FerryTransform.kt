package com.ssy.ferry

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform

abstract class FerryTransform : Transform() {
    override fun getName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isIncremental(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}