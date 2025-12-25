#include <com_elmfer_cnmcu_mcu_modules_CNUART.h>

//@line:94

         #include "cnmcuJava.h"
         #include "Nano.hpp"
     JNIEXPORT jlong JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNUART_size(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:99

        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        return uart->size();
    

}

JNIEXPORT void JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNUART_reset(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:104

        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        uart->reset();
    

}

JNIEXPORT jboolean JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNUART_shouldInterrupt(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:109

        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        return uart->shouldInterrupt();
    

}

JNIEXPORT jobject JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNUART_registerData(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:114

        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        return env->NewDirectByteBuffer(uart->registerData(), uart->size());
    

}

JNIEXPORT void JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNUART_write(JNIEnv* env, jclass clazz, jlong ptr, jint address, jint data) {


//@line:119

        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        uint16_t addr = address;
        uint16_t dat = data;
        uart->write(addr, dat);
    

}

JNIEXPORT jint JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNUART_read(JNIEnv* env, jclass clazz, jlong ptr, jint address) {


//@line:126

        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        uint16_t addr = address;
        return static_cast<jint>(uart->read(addr));
    

}

