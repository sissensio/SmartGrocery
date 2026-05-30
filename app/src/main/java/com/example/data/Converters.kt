package com.example.data

import androidx.room.TypeConverter
import com.example.api.GroupMemberResponse
import com.example.api.ShoppingListItemResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    private val memberListType = Types.newParameterizedType(List::class.java, GroupMemberResponse::class.java)
    private val memberListAdapter = moshi.adapter<List<GroupMemberResponse>>(memberListType)

    private val shoppingListItemListType = Types.newParameterizedType(List::class.java, ShoppingListItemResponse::class.java)
    private val shoppingListItemListAdapter = moshi.adapter<List<ShoppingListItemResponse>>(shoppingListItemListType)

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.let { stringListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return value?.let { stringListAdapter.fromJson(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromMemberList(value: List<GroupMemberResponse>?): String {
        return value?.let { memberListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toMemberList(value: String?): List<GroupMemberResponse> {
        return value?.let { memberListAdapter.fromJson(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromShoppingListItemList(value: List<ShoppingListItemResponse>?): String {
        return value?.let { shoppingListItemListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toShoppingListItemList(value: String?): List<ShoppingListItemResponse> {
        return value?.let { shoppingListItemListAdapter.fromJson(it) } ?: emptyList()
    }
}
