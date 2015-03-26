#ifndef _SCALAXY_STATIC_METHOD_HPP
#define _SCALAXY_STATIC_METHOD_HPP

#include "error.hpp"
#include "method_invocation.hpp"

namespace scalaxy {

  template <typename T>
  class static_method
  {
    jclass m_clazz;
    jmethodID m_method;

  public:
    static_method(JNIEnv *env, const char *class_name, const char *method_name, const char *signature) {
      jclass clazz = env->FindClass(class_name);
      m_clazz = reinterpret_cast<jclass>(env->NewGlobalRef(clazz));
      m_method = env->GetStaticMethodID(clazz, method_name, signature);
    }

    T invoke(JNIEnv *env, ...) {
      va_list args;
      va_start(args, env);
      T ret = method_invocation<T, true>::call_method(env, m_clazz, m_method, args);
      va_end(args);
      if (env->ExceptionCheck()) {
        throw scalaxy::error("An exception occurred while calling a static Java method");
      }
      return ret;
    }
  };

}  // namespace scalaxy

#endif  // _SCALAXY_STATIC_METHOD_HPP
