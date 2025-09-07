package com.elytelabs.labelscan

import com.google.gson.annotations.SerializedName

data class ProductResponse(
    @SerializedName("status") val status: Int?,
    @SerializedName("product") val product: Product?
)

data class Product(
    @SerializedName("product_name") val product_name: String?,
    @SerializedName("image_url") val image_url: String?,
    @SerializedName("ingredients_text") val ingredients_text: String?,
    @SerializedName("nutrient_levels") val nutrient_levels: NutrientLevels?
)

data class NutrientLevels(
    @SerializedName("fat") val fat: String?,
    @SerializedName("sugars") val sugars: String?,
    @SerializedName("salt") val salt: String?,
    @SerializedName("saturated-fat") val saturatedFat: String?
)
