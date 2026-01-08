#include "cnmcuJava.h"
#include <array>

JavaVM* cnmcuJava::vm;

jclass cnmcuJava::NullPointerException;
jclass cnmcuJava::IllegalArgumentException;
jclass cnmcuJava::IllegalStateException;
jclass cnmcuJava::RuntimeException;



jobject cnmcuJava::LOGGER;
jmethodID cnmcuJava::Logger_trace;
jmethodID cnmcuJava::Logger_debug;
jmethodID cnmcuJava::Logger_info;
jmethodID cnmcuJava::Logger_warn;
jmethodID cnmcuJava::Logger_error;

jclass cnmcuJava::Object;

jclass cnmcuJava::Boolean;
jmethodID cnmcuJava::Boolean_valueOf;

jclass cnmcuJava::Byte;
jmethodID cnmcuJava::Byte_valueOf;

jclass cnmcuJava::Short;
jmethodID cnmcuJava::Short_valueOf;

jclass cnmcuJava::Integer;
jmethodID cnmcuJava::Integer_valueOf;

jclass cnmcuJava::Long;
jmethodID cnmcuJava::Long_valueOf;

bool cnmcuJava::initialized = false;

void cnmcuJava::init(JNIEnv* env)
{
    if(initialized)
        return;

    env->GetJavaVM(&vm);

    jclass CodeNodeMicrocontrollers;
    jfieldID CodeNodeMicrocontrollers_LOGGER_id;

    // CodeNodeMicrocontrollers.LOGGER
    GET_CLASS(CodeNodeMicrocontrollers, "com/elmfer/cnmcu/CodeNodeMicrocontrollers");
    GET_STATIC_FIELD_ID(CodeNodeMicrocontrollers_LOGGER_id, CodeNodeMicrocontrollers, "LOGGER", "Lorg/slf4j/Logger;");
    LOGGER = env->GetStaticObjectField(CodeNodeMicrocontrollers, CodeNodeMicrocontrollers_LOGGER_id);
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

    initialized = true;

    cnmcuJava::debug("Natives initialised");
}

jobject cnmcuJava::convert_element(JNIEnv* env, char const &arg) {
    return env->NewStringUTF(&arg);
}

jobject cnmcuJava::convert_element(JNIEnv* env, std::string& arg) {
    return convert_element(env, arg.c_str());
}

jobject cnmcuJava::convert_element(JNIEnv* env, bool arg) {
    return env->CallStaticObjectMethod(Boolean, Boolean_valueOf, static_cast<jboolean>(arg));
}

jobject cnmcuJava::convert_element(JNIEnv* env, std::uint8_t arg) {
    return env->CallStaticObjectMethod(Byte, Byte_valueOf, static_cast<jbyte>(arg));
}

jobject cnmcuJava::convert_element(JNIEnv* env, std::uint16_t arg) {
    return env->CallStaticObjectMethod(Short, Short_valueOf, static_cast<jshort>(arg));
}

jobject cnmcuJava::convert_element(JNIEnv* env, std::uint32_t arg) {
    return env->CallStaticObjectMethod(Integer, Integer_valueOf, static_cast<jint>(arg));
}

jobject cnmcuJava::convert_element(JNIEnv* env, std::uint64_t arg) {
    return env->CallStaticObjectMethod(Long, Long_valueOf, static_cast<jlong>(arg));
}

jobject cnmcuJava::convert_element(JNIEnv* env, jobject arg) {
    return arg;
}