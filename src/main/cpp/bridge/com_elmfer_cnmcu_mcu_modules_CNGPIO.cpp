#include <com_elmfer_cnmcu_mcu_modules_CNGPIO.h>

//@line:144

        #include "cnmcuJava.h"
        #include "Nano.hpp"
    JNIEXPORT jlong JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNGPIO_size(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:149

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return static_cast<jlong>(gpio->size());
    

}

JNIEXPORT jobject JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNGPIO_pvFrontData(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:154

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return env->NewDirectByteBuffer(gpio->pvFrontData(), CodeNodeNano::GPIO_NUM_PINS);
    

}

JNIEXPORT jobject JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNGPIO_pvBackData(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:159

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return env->NewDirectByteBuffer(gpio->pvBackData(), CodeNodeNano::GPIO_NUM_PINS);
    

}

JNIEXPORT jobject JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNGPIO_dirData(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:164

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return env->NewDirectByteBuffer(gpio->dirData(), CodeNodeNano::GPIO_NUM_PINS / 8);
    

}

JNIEXPORT jobject JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNGPIO_intData(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:169

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return env->NewDirectByteBuffer(gpio->intData(), CodeNodeNano::GPIO_NUM_PINS / 2);
    

}

JNIEXPORT jobject JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNGPIO_iflData(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:174

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return env->NewDirectByteBuffer(gpio->iflData(), CodeNodeNano::GPIO_NUM_PINS / 8);
    

}

JNIEXPORT jint JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNGPIO_read(JNIEnv* env, jclass clazz, jlong ptr, jint address) {


//@line:179

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        uint8_t val = gpio->read(address);    
        return static_cast<jint>(val);
    

}

JNIEXPORT void JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNGPIO_write(JNIEnv* env, jclass clazz, jlong ptr, jint address, jint value) {


//@line:186

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        uint8_t val = static_cast<uint8_t>(value);
        gpio->write(addr, val);
    

}

JNIEXPORT jboolean JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNGPIO_shouldInterrupt(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:193

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return static_cast<jboolean>(gpio->shouldInterrupt());
    

}

