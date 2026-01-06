#include "cnmcuJava.h"

JNIEnv* cnmcuJava::env;
JavaVM* cnmcuJava::vm;

jclass cnmcuJava::NullPointerException;
jclass cnmcuJava::IllegalArgumentException;
jclass cnmcuJava::IllegalStateException;
jclass cnmcuJava::RuntimeException;



jobject cnmcuJava::LOGGER;
jmethodID cnmcuJava::Logger_debug;



jclass cnmcuJava::NanoMCU;

jclass cnmcuJava::MOS6502;
jmethodID cnmcuJava::MOS6502_init;

jclass cnmcuJava::CNGPIO;
jmethodID cnmcuJava::CNGPIO_init;

jclass cnmcuJava::CNRAM;
jmethodID cnmcuJava::CNRAM_init;

jclass cnmcuJava::CNROM;
jmethodID cnmcuJava::CNROM_init;

jclass cnmcuJava::CNEL;
jmethodID cnmcuJava::CNEL_init;

jclass cnmcuJava::CNUART;
jmethodID cnmcuJava::CNUART_init;

bool cnmcuJava::initialized = false;

void cnmcuJava::init(JNIEnv* env)
{
    if(initialized)
        return;

    cnmcuJava::env = env;
    env->GetJavaVM(&vm);


    // Exceptions
    GET_CLASS(NullPointerException, "java/lang/NullPointerException");
    GET_CLASS(IllegalArgumentException, "java/lang/IllegalArgumentException");
    GET_CLASS(IllegalStateException, "java/lang/IllegalStateException");
    GET_CLASS(RuntimeException, "java/lang/RuntimeException");

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
    GET_METHOD_ID(Logger_debug, Logger, "debug", "(Ljava/lang/String;)V");

    // For CNMCU
    GET_CLASS(NanoMCU, "com/elmfer/cnmcu/mcu/NanoMCU");

    GET_CLASS(MOS6502, "com/elmfer/cnmcu/mcu/cpu/MOS6502");
    GET_METHOD_ID(MOS6502_init, MOS6502, "<init>", "(J)V");

    GET_CLASS(CNGPIO, "com/elmfer/cnmcu/mcu/modules/CNGPIO");
    GET_METHOD_ID(CNGPIO_init, CNGPIO, "<init>", "(J)V");

    GET_CLASS(CNRAM, "com/elmfer/cnmcu/mcu/modules/CNRAM");
    GET_METHOD_ID(CNRAM_init, CNRAM, "<init>", "(J)V");

    GET_CLASS(CNROM, "com/elmfer/cnmcu/mcu/modules/CNROM");
    GET_METHOD_ID(CNROM_init, CNROM, "<init>", "(J)V");

    GET_CLASS(CNEL, "com/elmfer/cnmcu/mcu/modules/CNEL");
    GET_METHOD_ID(CNEL_init, CNEL, "<init>", "(J)V");

    GET_CLASS(CNUART, "com/elmfer/cnmcu/mcu/modules/CNUART");
    GET_METHOD_ID(CNUART_init, CNUART, "<init>", "(J)V");

    initialized = true;
}

void cnmcuJava::debug_printf(const char* format, ...)
{
    if(!initialized)
        return;

    va_list args;
    va_start(args, format);

    char buffer[512] = {0};
    vsnprintf(buffer, sizeof(buffer) - 1, format, args);

    va_end(args);

    jstring str = env->NewStringUTF(buffer);
    env->CallVoidMethod(LOGGER, Logger_debug, str);
    env->DeleteLocalRef(str);
}