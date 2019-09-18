package com.ssy.ferry

import com.android.build.gradle.api.BaseVariant

interface VariantProcessor {

    fun process(variant: BaseVariant)

}