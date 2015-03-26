#ifndef _SCALAXY_ERROR_HPP
#define _SCALAXY_ERROR_HPP

namespace scalaxy {

  class error {
    const char *m_message;
  public:
    error(const char *message) : m_message(message) { }
    void throw_if_needed(JNIEnv *env) const {
      if (env->ExceptionCheck()) {
        return;
      }
      jclass clazz = env->FindClass("java/lang/RuntimeException");
      env->ThrowNew(clazz, m_message);
    }
    static void caught_unexpected(JNIEnv *env) {
      error("Unexpected native error!").throw_if_needed(env);
    }
  };

}  // namespace scalaxy

#endif  // _SCALAXY_ERROR_HPP
