#include <com_elmfer_cnmcu_mcu_modules_CNRAM.h>

//@line:79

         #include "cnmcuJava.h"
         #include "Nano.hpp"
     JNIEXPORT jlong JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNRAM_size(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:84

        return static_cast<jlong>(CodeNodeNano::RAM_SIZE);
    

}

JNIEXPORT jobject JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNRAM_data(JNIEnv* env, jclass clazz, jlong ptr) {


//@line:88

        CNRAM<CodeNodeNano::RAM_SIZE>* ram = reinterpret_cast<CNRAM<CodeNodeNano::RAM_SIZE>*>(ptr);
        return env->NewDirectByteBuffer(ram->data(), CodeNodeNano::RAM_SIZE);
    

}

JNIEXPORT jbyte JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNRAM_read(JNIEnv* env, jclass clazz, jlong ptr, jint address) {


//@line:93

        CNRAM<CodeNodeNano::RAM_SIZE>* ram = reinterpret_cast<CNRAM<CodeNodeNano::RAM_SIZE>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        return static_cast<jbyte>(ram->read(addr));
    

}

JNIEXPORT void JNICALL Java_com_elmfer_cnmcu_mcu_modules_CNRAM_write(JNIEnv* env, jclass clazz, jlong ptr, jint address, jbyte value) {


//@line:99

        CNRAM<CodeNodeNano::RAM_SIZE>* ram = reinterpret_cast<CNRAM<CodeNodeNano::RAM_SIZE>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        uint8_t val = static_cast<uint8_t>(value);
        ram->write(addr, val);
    

}

