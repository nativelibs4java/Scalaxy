#ifndef _SCALAXY_LOCAL_ARRAY_HPP
#define _SCALAXY_LOCAL_ARRAY_HPP

#include "jni_type.hpp"

namespace scalaxy {

  template <typename T>
  class local_array
  {
    JNIEnv *m_env;
    typename jni_type<T>::array_type m_arr;
    T *m_ptr;
    jint m_mode;

  public:
    local_array(JNIEnv *env, jobject arr, jint mode = 0) :
        local_array(env, reinterpret_cast<typename jni_type<T>::array_type>(arr), mode) { }

    local_array(JNIEnv *env, typename jni_type<T>::array_type arr, jint mode = 0) {
      m_env = env;
      m_arr = arr;
      m_mode = mode;
      m_ptr = jni_type<T>::get_array_elements(env, arr);
    }

    ~local_array() {
      jni_type<T>::release_array_elements(m_env, m_arr, m_ptr, m_mode);
    }

    T* operator*() const { return m_ptr; }
    size_t size() {
      return m_env->GetArrayLength(m_arr);
    }
    T& operator[](size_t i) {
      return m_ptr[i];
    }
  };

}  // namespace scalaxy

#endif  // _SCALAXY_LOCAL_ARRAY_HPP
