/*
 * $Id: com_threerings_util_unsafe_Unsafe.c 3331 2005-02-03 01:25:21Z mdb $
 */

#include <stdio.h>
#include <jni.h>
#include <jvmpi.h>

#include "com_threerings_util_unsafe_UnsafeGC.h"

/* global jvmpi interface pointer */
static JVMPI_Interface* jvmpi;

JNIEXPORT void JNICALL
Java_com_threerings_util_unsafe_Unsafe_enableGC (JNIEnv* env, jclass clazz)
{
    fprintf(stderr, "Reenabling GC.\n");
    jvmpi->EnableGC();
}

JNIEXPORT void JNICALL
Java_com_threerings_util_unsafe_Unsafe_disableGC (JNIEnv* env, jclass clazz)
{
    fprintf(stderr, "Disabling GC.\n");
    jvmpi->DisableGC();
}

JNIEXPORT jboolean JNICALL
Java_com_threerings_util_unsafe_Unsafe_init (JNIEnv* env, jclass clazz)
{
    JavaVM* jvm;

    if ((*env)->GetJavaVM(env, &jvm) > 0) {
        fprintf(stderr, "Failed to get JavaVM from env.\n");
        return JNI_FALSE;
    }

    /* get jvmpi interface pointer */
    if (((*jvm)->GetEnv(jvm, (void**)&jvmpi, JVMPI_VERSION_1)) < 0) {
        fprintf(stderr, "Failed to get JVMPI from JavaVM.\n");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
