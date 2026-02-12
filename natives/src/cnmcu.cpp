#include "cnmcu.hpp"

JavaVM* vm;

jclass NullPointerException;
jclass IllegalArgumentException;
jclass IllegalStateException;
jclass RuntimeException;



jobject LOGGER;
jmethodID Logger_trace;
jmethodID Logger_debug;
jmethodID Logger_info;
jmethodID Logger_warn;
jmethodID Logger_error;

jclass Object;

jclass Boolean;
jmethodID Boolean_valueOf;

jclass Byte;
jmethodID Byte_valueOf;

jclass Short;
jmethodID Short_valueOf;

jclass Integer;
jmethodID Integer_valueOf;

jclass Long;
jmethodID Long_valueOf;

void init(JavaVM* vm)
{
    ::vm = vm;

    JNIEnv* env;

    GET_ENV(env, vm);

    jclass CNMCU;
    jfieldID CNMCU_LOGGER_id;

    // CNMCU.LOGGER
    GET_CLASS(CNMCU, "com/elmfer/cnmcu/CNMCU");
    GET_STATIC_FIELD_ID(CNMCU_LOGGER_id, CNMCU, "LOGGER", "Lorg/slf4j/Logger;");
    LOGGER = env->GetStaticObjectField(CNMCU, CNMCU_LOGGER_id);
    LOGGER = env->NewGlobalRef(LOGGER);

    // Logger.info
    jclass Logger;
    GET_CLASS(Logger, "org/slf4j/Logger");
    GET_METHOD_ID(Logger_trace, Logger, "trace", "(Ljava/lang/String;[Ljava/lang/Object;)V");
    GET_METHOD_ID(Logger_debug, Logger, "debug", "(Ljava/lang/String;[Ljava/lang/Object;)V");
    GET_METHOD_ID(Logger_info, Logger, "info", "(Ljava/lang/String;[Ljava/lang/Object;)V");
    GET_METHOD_ID(Logger_warn, Logger, "warn", "(Ljava/lang/String;[Ljava/lang/Object;)V");
    GET_METHOD_ID(Logger_error, Logger, "error", "(Ljava/lang/String;[Ljava/lang/Object;)V");

    GET_CLASS(Object, "java/lang/Object");

    GET_CLASS(Boolean, "java/lang/Boolean");
    GET_STATIC_METHOD_ID(Boolean_valueOf, Boolean, "valueOf", "(Z)Ljava/lang/Boolean;");

    GET_CLASS(Byte, "java/lang/Byte");
    GET_STATIC_METHOD_ID(Byte_valueOf, Byte, "valueOf", "(B)Ljava/lang/Byte;");

    GET_CLASS(Short, "java/lang/Short");
    GET_STATIC_METHOD_ID(Short_valueOf, Short, "valueOf", "(S)Ljava/lang/Short;");

    GET_CLASS(Integer, "java/lang/Integer");
    GET_STATIC_METHOD_ID(Integer_valueOf, Integer, "valueOf", "(I)Ljava/lang/Integer;");

    GET_CLASS(Long, "java/lang/Long");
    GET_STATIC_METHOD_ID(Long_valueOf, Long, "valueOf", "(J)Ljava/lang/Long;");

    debug("Natives initialised");
}

jobject convert_element(JNIEnv* env, char const &arg) {
    return env->NewStringUTF(&arg);
}

jobject convert_element(JNIEnv* env, std::string& arg) {
    return convert_element(env, arg.c_str());
}

jobject convert_element(JNIEnv* env, bool arg) {
    return env->CallStaticObjectMethod(Boolean, Boolean_valueOf, static_cast<jboolean>(arg));
}

jobject convert_element(JNIEnv* env, std::uint8_t arg) {
    return env->CallStaticObjectMethod(Byte, Byte_valueOf, static_cast<jbyte>(arg));
}

jobject convert_element(JNIEnv* env, std::uint16_t arg) {
    return env->CallStaticObjectMethod(Short, Short_valueOf, static_cast<jshort>(arg));
}

jobject convert_element(JNIEnv* env, std::uint32_t arg) {
    return env->CallStaticObjectMethod(Integer, Integer_valueOf, static_cast<jint>(arg));
}

jobject convert_element(JNIEnv* env, std::uint64_t arg) {
    return env->CallStaticObjectMethod(Long, Long_valueOf, static_cast<jlong>(arg));
}

jobject convert_element(JNIEnv* env, jobject arg) {
    return arg;
}

extern "C" {
    jint JNI_OnLoad(JavaVM* vm, void*) {
        init(vm);
        return TARGET_VERSION;
    }
}