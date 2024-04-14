package com.terfess.samp_launcher.core

import android.util.Log
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Arrays

object Utils {
    fun BytesToMB(Bytes: Float): Float {
        return Bytes / 1048576 // (1024 * 1024)
    }

    fun GetFileExtensions(file: File): ArrayList<String> {
        val List: ArrayList<String>
        val FileName: String = file.getName().toLowerCase()

        // Split by dot
        List = ArrayList(Arrays.asList(*FileName.split("\\.".toRegex()).toTypedArray()))
        if (!List.isEmpty()) List.removeAt(0)
        return List
    }

    @JvmStatic
    fun GetFileLastExtension(file: File): String {
        val Result: ArrayList<String> = GetFileExtensions(file)
        return if (Result.isEmpty()) "" else Result.get(Result.size - 1)
    }

    @JvmStatic
    fun GetFileNameWithoutExtension(file: File, ConvertToLowercase: Boolean): String {
        if (ConvertToLowercase) return file.getName().toLowerCase().split("\\.".toRegex())
            .dropLastWhile({ it.isEmpty() }).toTypedArray().get(0)
        return file.getName().split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            .get(0)
    }

    @JvmStatic
    fun RemoveFile(file: File?): Boolean {
        if (file != null) return file.delete()
        return false
    }

    @JvmStatic
    fun <ObjType : Any> DeepCloneObject(Object: ObjType): ObjType? {
        try {
            val Clone: Any = Object.javaClass.newInstance()
            for (field: Field in Object.javaClass.getDeclaredFields()) {
                field.setAccessible(true)

                // Skip if filed is null or final
                if (field.get(Object) == null || Modifier.isFinal(field.getModifiers())) {
                    continue
                }
                if ((field.getType()
                        .isPrimitive() || (field.getType() == String::class.java) || (field.getType()
                        .getSuperclass() == Number::class.java) || (field.getType() == Boolean::class.java))
                ) {
                    field.set(Clone, field.get(Object))
                } else {
                    val childObj: Any? = field.get(Object)
                    if (childObj === Object) { // Self-reference check
                        field.set(Clone, Clone)
                    } else {
                        field.set(Clone, DeepCloneObject(field.get(Object)))
                    }
                }
            }
            return Clone as ObjType
        } catch (e: Exception) {
            Log.e("DeepClone", "Failed to clone object - " + Object.javaClass.getName())
            return null
        }
    }
}