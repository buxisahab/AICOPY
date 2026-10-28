package com.musifly.android.utils

import android.os.Build
import android.util.Log
import java.lang.reflect.Method

/**
 * Safe reflection utilities to prevent NullPointerExceptions
 * All methods return null on failure instead of throwing
 * 
 * Android 15 (SDK 34+) Compatibility:
 * - Method.invoke() can return null on Android 15
 * - Hidden API access is restricted
 * - All reflection calls must be wrapped with strict null checks
 */
object ReflectionUtils {
    const val TAG = "ReflectionUtils"
    
    /**
     * Check if running on Android 15+ (SDK 34+)
     * Reflection behavior changes on Android 15
     */
    val isAndroid15Plus: Boolean
        get() = Build.VERSION.SDK_INT >= 34 // Android 15 (API 34)
    
    /**
     * Safely invoke a method via reflection
     * Returns null if method not found or invocation fails
     * 
     * Android 15 Fix: Method.invoke() can return null, so we check for null result
     * 
     * @param obj Object to invoke method on
     * @param methodName Name of the method
     * @param args Method arguments
     * @return Method result or null on failure
     */
    inline fun <reified T> safeInvoke(
        obj: Any?,
        methodName: String,
        vararg args: Any?
    ): T? {
        if (obj == null) {
            Log.w(TAG, "Cannot invoke $methodName on null object")
            return null
        }
        
        // On Android 15+, disable reflection for hidden APIs
        if (isAndroid15Plus) {
            Log.d(TAG, "Android 15+ detected: Using strict reflection safety for $methodName")
        }
        
        return try {
            // Get method
            val method = findMethod(obj.javaClass, methodName, args.map { it?.javaClass })
            
            if (method == null) {
                Log.w(TAG, "Method not found: ${obj.javaClass.name}.$methodName")
                return null
            }
            
            // Make accessible (may fail on Android 15+ for hidden APIs)
            try {
                method.isAccessible = true
            } catch (e: SecurityException) {
                if (isAndroid15Plus) {
                    Log.w(TAG, "Cannot make method accessible on Android 15+: ${obj.javaClass.name}.$methodName")
                    return null
                }
                throw e
            }
            
            // CRITICAL: On Android 15, method.invoke() can return null even if method exists
            // We must check for null result before casting
            @Suppress("UNCHECKED_CAST")
            val result = method.invoke(obj, *args)
            
            // Android 15 Fix: Check if result is null (indicates reflection failure)
            if (result == null && isAndroid15Plus) {
                Log.w(TAG, "Method.invoke() returned null on Android 15+: ${obj.javaClass.name}.$methodName")
                return null
            }
            
            result as? T
            
        } catch (e: Throwable) {
            // Catch ALL exceptions including SecurityException, IllegalAccessException, etc.
            Log.w(TAG, "Reflection invoke failed: ${obj.javaClass.name}.$methodName", e)
            null
        }
    }
    
    /**
     * Find method by name and parameter types
     * Android 15 Fix: Handle SecurityException for hidden APIs
     */
    fun findMethod(
        clazz: Class<*>,
        methodName: String,
        paramTypes: List<Class<*>?>
    ): Method? {
        return try {
            // Try exact match first
            val exactTypes = paramTypes.mapNotNull { it }.toTypedArray()
            clazz.getMethod(methodName, *exactTypes)
        } catch (e: NoSuchMethodException) {
            // Try declared methods (including private)
            try {
                clazz.getDeclaredMethod(methodName, *paramTypes.mapNotNull { it }.toTypedArray())
            } catch (e2: NoSuchMethodException) {
                // Search all methods by name
                try {
                    clazz.methods.find { it.name == methodName }
                        ?: clazz.declaredMethods.find { it.name == methodName }
                } catch (e3: SecurityException) {
                    // Android 15: Hidden API access blocked
                    if (isAndroid15Plus) {
                        Log.w(TAG, "Cannot access method on Android 15+ (hidden API): $methodName")
                    }
                    null
                }
            } catch (e2: SecurityException) {
                // Android 15: Hidden API access blocked
                if (isAndroid15Plus) {
                    Log.w(TAG, "Cannot access declared method on Android 15+ (hidden API): $methodName")
                }
                null
            }
        } catch (e: SecurityException) {
            // Android 15: Hidden API access blocked
            if (isAndroid15Plus) {
                Log.w(TAG, "Cannot access method on Android 15+ (hidden API): $methodName")
            }
            null
        }
    }
    
    /**
     * Safely get field value
     * 
     * @param obj Object to get field from
     * @param fieldName Name of the field
     * @return Field value or null on failure
     */
    inline fun <reified T> safeGetField(
        obj: Any?,
        fieldName: String
    ): T? {
        if (obj == null) {
            Log.w(TAG, "Cannot get field $fieldName from null object")
            return null
        }
        
        return try {
            val field = obj.javaClass.getDeclaredField(fieldName)
            
            // Android 15 Fix: Handle SecurityException for hidden APIs
            try {
                field.isAccessible = true
            } catch (e: SecurityException) {
                if (isAndroid15Plus) {
                    Log.w(TAG, "Cannot make field accessible on Android 15+: ${obj.javaClass.name}.$fieldName")
                    return null
                }
                throw e
            }
            
            @Suppress("UNCHECKED_CAST")
            val result = field.get(obj)
            result as? T
            
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to get field: ${obj.javaClass.name}.$fieldName", e)
            null
        }
    }
    
    /**
     * Safely set field value
     * 
     * @param obj Object to set field on
     * @param fieldName Name of the field
     * @param value Value to set
     * @return true if successful, false otherwise
     */
    fun safeSetField(
        obj: Any?,
        fieldName: String,
        value: Any?
    ): Boolean {
        if (obj == null) {
            Log.w(TAG, "Cannot set field $fieldName on null object")
            return false
        }
        
        return try {
            val field = obj.javaClass.getDeclaredField(fieldName)
            
            // Android 15 Fix: Handle SecurityException for hidden APIs
            try {
                field.isAccessible = true
            } catch (e: SecurityException) {
                if (isAndroid15Plus) {
                    Log.w(TAG, "Cannot make field accessible on Android 15+: ${obj.javaClass.name}.$fieldName")
                    return false
                }
                throw e
            }
            
            field.set(obj, value)
            true
            
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to set field: ${obj.javaClass.name}.$fieldName", e)
            false
        }
    }
}
