#ifndef _SCALAXY_METHOD_HPP
#define _SCALAXY_METHOD_HPP

#include "error.hpp"
#include "method_invocation.hpp"

namespace scalaxy {

  template <typename T>
  class method
  {
    jmethodID m_method;

  public:
    method(JNIEnv *env, const char *class_name, const char *method_name, const char *signature) {
      jclass clazz = env->FindClass(class_name);
      m_method = env->GetMethodID(clazz, method_name, signature);
    }

    T invoke(JNIEnv *env, jobject target, ...) const {
      va_list args;
      va_start(args, target);
      T ret = method_invocation<T, false>::call_method(env, target, m_method, args);
      va_end(args);
      if (env->ExceptionCheck()) {
        throw scalaxy::error("An exception occurred while calling a Java method");
      }
      return ret;
    }
  };

}  // namespace scalaxy

#endif  // _SCALAXY_METHOD_HPP
