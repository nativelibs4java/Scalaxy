#ifndef _SCALAXY_METHOD_INVOCATION_HPP
#define _SCALAXY_METHOD_INVOCATION_HPP

namespace scalaxy {

  template <typename T, bool is_static>
  struct method_invocation {
  };

  #define _METHOD_INVOCATION(type, is_static, staticOrEmpty, targetType, name) \
    template <> \
    struct method_invocation<type, is_static> { \
    public: \
      static type call_method(JNIEnv *env, targetType target, jmethodID methodID, ...) { \
        va_list args; \
        va_start(args, methodID); \
        type ret = env->Call ## staticOrEmpty ## name ## Method(target, methodID, args); \
        va_end(args); \
        return ret; \
      } \
    } \

  #define METHOD_INVOCATION(type, name) \
    _METHOD_INVOCATION(type, true, Static, jclass, name); \
    _METHOD_INVOCATION(type, false,, jobject, name); \

  METHOD_INVOCATION(jint, Int)
  METHOD_INVOCATION(jlong, Long)
  METHOD_INVOCATION(jshort, Short)
  METHOD_INVOCATION(jbyte, Byte)
  METHOD_INVOCATION(jchar, Char)
  METHOD_INVOCATION(jboolean, Boolean)
  METHOD_INVOCATION(jfloat, Float)
  METHOD_INVOCATION(jdouble, Double)
  METHOD_INVOCATION(jobject, Object)

}  // namespace scalaxy

#endif  // _SCALAXY_METHOD_INVOCATION_HPP
