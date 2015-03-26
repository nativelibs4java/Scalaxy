#ifndef _SCALAXY_JNI_TYPE_HPP
#define _SCALAXY_JNI_TYPE_HPP

namespace scalaxy {

  template <typename T>
  struct jni_type {
    typedef void array_type;
  };

  #define JNI_TYPE(type, name) \
    template <> \
    struct jni_type<type> { \
    public: \
      typedef type ## Array array_type; \
      static type *get_array_elements(JNIEnv *env, array_type array) { \
        return env->Get ## name ## ArrayElements(array, NULL); \
      } \
      static void release_array_elements(JNIEnv *env, array_type array, type *ptr, jint mode) { \
        env->Release ## name ## ArrayElements(array, ptr, mode); \
      } \
    } \

  JNI_TYPE(jint, Int);
  JNI_TYPE(jlong, Long);
  JNI_TYPE(jshort, Short);
  JNI_TYPE(jbyte, Byte);
  JNI_TYPE(jchar, Char);
  JNI_TYPE(jboolean, Boolean);
  JNI_TYPE(jfloat, Float);
  JNI_TYPE(jdouble, Double);

  template <>
  struct jni_type<jobject> {
    typedef jobjectArray array_type;
  };

}  // namespace scalaxy

#endif  // _SCALAXY_JNI_TYPE_HPP
