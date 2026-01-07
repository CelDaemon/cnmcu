#pragma once

#include <jni.h>
#include <cstdint>
#include <algorithm>
#include <iostream>

#define CHECK_FOR_EXCEPTION() \
    if(env->ExceptionCheck()) \
    { \
        env->ExceptionDescribe(); \
        env->ExceptionClear(); \
        return; \
    } \

#define GET_CLASS(var, name) \
    (var) = env->FindClass(name); \
    CHECK_FOR_EXCEPTION(); \
    (var) = (jclass) env->NewGlobalRef(var); \
    CHECK_FOR_EXCEPTION(); \

#define GET_METHOD_ID(var, clazz, name, sig) \
    (var) = env->GetMethodID(clazz, name, sig); \
    CHECK_FOR_EXCEPTION(); \

#define GET_STATIC_METHOD_ID(var, clazz, name, sig) \
    (var) = env->GetStaticMethodID(clazz, name, sig); \
    CHECK_FOR_EXCEPTION(); \

#define GET_FIELD_ID(var, clazz, name, sig) \
    (var) = env->GetFieldID(clazz, name, sig); \
    CHECK_FOR_EXCEPTION(); \

#define GET_STATIC_FIELD_ID(var, clazz, name, sig) \
    (var) = env->GetStaticFieldID(clazz, name, sig); \
    CHECK_FOR_EXCEPTION(); \

class cnmcuJava
{
    static jobject convert_element(char const & arg);
    static jobject convert_element(std::string& arg);
    static jobject convert_element(bool arg);
    static jobject convert_element(std::uint8_t arg);
    static jobject convert_element(std::uint16_t arg);
    static jobject convert_element(std::uint32_t arg);
    static jobject convert_element(std::uint64_t arg);
    static jobject convert_element(jobject arg);

    template<size_t N>
    static jobject convert_element(std::array<int, N> const & arg) {
        std::array<jint, N> converted_arg;
        std::copy(arg.cbegin(), arg.cend(), converted_arg.begin());
        jintArray array = env->NewIntArray(N);
        env->SetIntArrayRegion(array, 0, N, converted_arg.data());
        return array;
    }

    template<typename T>
    static void write_element(jobjectArray array, jsize index, T arg) {
        auto const elem = convert_element(arg);
        env->SetObjectArrayElement(array, index, elem);
        env->DeleteLocalRef(elem);
    }

    template<typename... Args>
    static void log(jmethodID method, const char* format, Args&&... args)
    {
        if(!initialized)
            return;

        jobjectArray log_args = env->NewObjectArray(sizeof...(args), Object, NULL);
        if(log_args == NULL) {
            std::cerr << "Failed to create log args array" << std::endl;
            return;
        }
        jsize index;
        (write_element(log_args, index++, args), ...);

        jstring str = env->NewStringUTF(format);
        env->CallVoidMethod(LOGGER, method, str, log_args);
        env->DeleteLocalRef(str);
        env->DeleteLocalRef(log_args);
    }

public:
    static JNIEnv* env;
    static JavaVM* vm;


    // Exceptions
    static jclass NullPointerException;
    static jclass IllegalArgumentException;
    static jclass IllegalStateException;
    static jclass RuntimeException;


    // Logger
    static jobject LOGGER;
    static jmethodID Logger_trace;
    static jmethodID Logger_debug;
    static jmethodID Logger_info;
    static jmethodID Logger_warn;
    static jmethodID Logger_error;


    // For CNMCU
    static jclass NanoMCU;

    static jclass MOS6502;
    static jmethodID MOS6502_init;

    static jclass CNGPIO;
    static jmethodID CNGPIO_init;

    static jclass CNRAM;
    static jmethodID CNRAM_init;

    static jclass CNROM;
    static jmethodID CNROM_init;

    static jclass CNEL;
    static jmethodID CNEL_init;

    static jclass CNUART;
    static jmethodID CNUART_init;

    static jclass Object;

    static jclass Boolean;
    static jmethodID Boolean_valueOf;

    static jclass Byte;
    static jmethodID Byte_valueOf;

    static jclass Short;
    static jmethodID Short_valueOf;

    static jclass Integer;
    static jmethodID Integer_valueOf;

    static jclass Long;
    static jmethodID Long_valueOf;

    static bool initialized;

    static void init(JNIEnv* env);

    template<typename... Args>
    static void trace(const char* format, Args&&... args) {
        return log(Logger_trace, format, args...);
    }

    template<typename... Args>
    static void debug(const char* format, Args&&... args) {
        return log(Logger_debug, format, args...);
    }

    template<typename... Args>
    static void info(const char* format, Args&&... args) {
        return log(Logger_info, format, args...);
    }

    template<typename... Args>
    static void warn(const char* format, Args&&... args) {
        return log(Logger_warn, format, args...);
    }

    template<typename... Args>
    static void error(const char* format, Args&&... args) {
        return log(Logger_error, format, args...);
    }
};