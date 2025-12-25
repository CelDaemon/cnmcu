#include <com_elmfer_cnmcu_mcu_modules_CNEL.h>

//@line:121

         #include "cnmcuJava.h"
         #include "Nano.hpp"
     JNIEXPORT jlong JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNEL_size(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:126

        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        return cnel->size();
    

}

JNIEXPORT jobject JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNEL_iclRegistersData(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:131

        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        return env->NewDirectByteBuffer(cnel->iclRegistersData(), CodeNodeNano::EL_SIZE);
    

}

JNIEXPORT jobject JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNEL_iflRegistersData(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:136

        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        return env->NewDirectByteBuffer(cnel->iflRegistersData(), CodeNodeNano::EL_SIZE);
    

}

JNIEXPORT void JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNEL_triggerEvent(JNIEnv* env, jclass clazz, jlong ptr, jint event) {


//@line:141

        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        cnel->triggerEvent(event);
    

}

JNIEXPORT jboolean JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNEL_shouldInterrupt(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:146

        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        return cnel->shouldInterrupt();
    

}

JNIEXPORT jint JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNEL_read(JNIEnv* env, jclass clazz, jlong ptr, jint address) {


//@line:151

        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        return static_cast<jint>(cnel->read(address));
    

}

JNIEXPORT void JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNEL_write(JNIEnv* env, jclass clazz, jlong ptr, jint address, jint data) {


//@line:156

        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        uint8_t dat = static_cast<uint8_t>(data);
        cnel->write(addr, dat);
    

}

