#pragma once

#include <jni.h>
#include <cstdint>
#include <string>
#include <cstdlib>

#define TARGET_VERSION JNI_VERSION_21

#define CHECK_FOR_EXCEPTION() \
    if(env->ExceptionCheck()) \
    { \
        env->ExceptionDescribe(); \
        env->ExceptionClear(); \
        env->FatalError(NULL); \
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

#define GET_ENV(var, vm) \
    if(vm->GetEnv(reinterpret_cast<void**>(&(var)), TARGET_VERSION) != JNI_OK) \
        abort(); \

extern JavaVM* vm;


// Exceptions
extern jclass NullPointerException;
extern jclass IllegalArgumentException;
extern jclass IllegalStateException;
extern jclass RuntimeException;


// Logger
extern jobject LOGGER;
extern jmethodID Logger_trace;
extern jmethodID Logger_debug;
extern jmethodID Logger_info;
extern jmethodID Logger_warn;
extern jmethodID Logger_error;

extern jclass Object;

extern jclass Boolean;
extern jmethodID Boolean_valueOf;

extern jclass Byte;
extern jmethodID Byte_valueOf;

extern jclass Short;
extern jmethodID Short_valueOf;

extern jclass Integer;
extern jmethodID Integer_valueOf;

extern jclass Long;
extern jmethodID Long_valueOf;

jobject convert_element(JNIEnv* env, char const & arg);
jobject convert_element(JNIEnv* env, std::string& arg);
jobject convert_element(JNIEnv* env, bool arg);
jobject convert_element(JNIEnv* env, std::uint8_t arg);
jobject convert_element(JNIEnv* env, std::uint16_t arg);
jobject convert_element(JNIEnv* env, std::uint32_t arg);
jobject convert_element(JNIEnv* env, std::uint64_t arg);
jobject convert_element(JNIEnv* env, jobject arg);

template<size_t N>
jobject convert_element(JNIEnv* env, std::array<int, N> const & arg) {
    std::array<jint, N> converted_arg;
    std::copy(arg.cbegin(), arg.cend(), converted_arg.begin());
    jintArray array = env->NewIntArray(N);
    env->SetIntArrayRegion(array, 0, N, converted_arg.data());
    return array;
}

template<typename T>
void write_element(JNIEnv* env, jobjectArray array, jsize index, T arg) {
    auto const elem = convert_element(env, arg);
    env->SetObjectArrayElement(array, index, elem);
    env->DeleteLocalRef(elem);
}

template<typename... Args>
void log(jmethodID method, const char* format, Args&&... args)
{

    JNIEnv* env;
    GET_ENV(env, vm);

    jobjectArray log_args = env->NewObjectArray(sizeof...(args), Object, NULL);
    if(log_args == NULL)
        abort();
    jsize index;
    (write_element(env, log_args, index++, args), ...);

    jstring str = env->NewStringUTF(format);
    env->CallVoidMethod(LOGGER, method, str, log_args);
    env->DeleteLocalRef(str);
    env->DeleteLocalRef(log_args);
}

void init(JavaVM* vm);

template<typename... Args>
void trace(const char* format, Args&&... args) {
    return log(Logger_trace, format, args...);
}

template<typename... Args>
void debug(const char* format, Args&&... args) {
    return log(Logger_debug, format, args...);
}

template<typename... Args>
void info(const char* format, Args&&... args) {
    return log(Logger_info, format, args...);
}

template<typename... Args>
void warn(const char* format, Args&&... args) {
    return log(Logger_warn, format, args...);
}

template<typename... Args>
void error(const char* format, Args&&... args) {
    return log(Logger_error, format, args...);
}