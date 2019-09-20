package com.ssy.ferry

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.variant.BaseVariantData

/**
 * 2019-09-20
 * @author Mr.S
 */
val BaseVariant.variantData: BaseVariantData
    get() = (this as ApplicationVariantImpl).variantData