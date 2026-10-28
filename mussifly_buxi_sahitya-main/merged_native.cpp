// Generated Stack Strings
#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <android/log.h>
#include <unistd.h>

// Helper to convert jstring to std::string
std::string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr) return "";
    const char *chars = env->GetStringUTFChars(jStr, nullptr);
    std::string ret(chars);
    env->ReleaseStringUTFChars(jStr, chars);
    return ret;
}

// Base64 Decryption Logic
static const std::string base64_chars =
             "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
             "abcdefghijklmnopqrstuvwxyz"
             "0123456789+/";

static inline bool is_base64(unsigned char c) {
  return (isalnum(c) || (c == '+') || (c == '/'));
}

std::vector<unsigned char> base64_decode(std::string const& encoded_string) {
  int in_len = encoded_string.size();
  int i = 0;
  int j = 0;
  int in_ = 0;
  unsigned char char_array_4[4], char_array_3[3];
  std::vector<unsigned char> ret;

  while (in_len-- && ( encoded_string[in_] != '=') && is_base64(encoded_string[in_])) {
    char_array_4[i++] = encoded_string[in_]; in_++;
    if (i ==4) {
      for (i = 0; i <4; i++)
        char_array_4[i] = base64_chars.find(char_array_4[i]);

      char_array_3[0] = (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
      char_array_3[1] = ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
      char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];

      for (i = 0; (i < 3); i++)
        ret.push_back(char_array_3[i]);
      i = 0;
    }
  }

  if (i) {
    for (j = i; j <4; j++)
      char_array_4[j] = 0;

    for (j = 0; j <4; j++)
      char_array_4[j] = base64_chars.find(char_array_4[j]);

    char_array_3[0] = (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
    char_array_3[1] = ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
    char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];

    for (j = 0; (j < i - 1); j++) ret.push_back(char_array_3[j]);
  }

  return ret;
}

std::string get_dynamic_key() {
    // Key: D3epM1ndSec!
    char k[] = { 68, 51, 101, 112, 77, 49, 110, 100, 83, 101, 99, 33, 0 };
    return std::string(k);
}

std::string decrypt_string(const char* b64_input) {
    std::string key = get_dynamic_key();
    std::vector<unsigned char> decoded = base64_decode(std::string(b64_input));
    std::string result = "";
    int key_len = key.length();
    for (size_t i = 0; i < decoded.size(); ++i) {
        result += (char)(decoded[i] ^ key[i % key_len]);
    }
    return result;
}

std::string get_url_by_id(int id) {
    switch(id) {
        case 1: {
    char INSTA_B64[] = { 'L', 'E', 'c', 'R', 'A', 'D', '4', 'L', 'Q', 'U', 's', '6', 'C', 'x', 'B', 'V', 'J', 'V', 'Q', 'X', 'E', 'S', 'A', 'f', 'D', 'Q', 's', '+', 'S', 'h', 'B', 'A', 'L', 'F', 'o', 'R', 'C', 'S', 'x', 'u', 'X', 'l', 'R', 'j', 0 };
            return decrypt_string(INSTA_B64);
        }
        case 2: {
    char INSTA_WWW_B64[] = { 'L', 'E', 'c', 'R', 'A', 'D', '4', 'L', 'Q', 'U', 's', 'k', 'E', 'h', 'Q', 'P', 'L', 'V', '0', 'W', 'B', 'C', 'x', 'W', 'H', 'A', 'U', '+', 'S', 'w', 'B', 'O', 'K', 'R', 'w', 'W', 'E', 'S', 'V', 'Y', 'G', 'h', '0', 'y', 'O', 'l', 'M', 'R', 'd', 'A', '=', '=', 0 };
            return decrypt_string(INSTA_WWW_B64);
        }
        case 3: {
    char TELEGRAM_B64[] = { 'L', 'E', 'c', 'R', 'A', 'D', '4', 'L', 'Q', 'U', 's', 'n', 'S', 'w', '5', 'E', 'a', '3', '4', 'Q', 'A', 'z', '5', 'Y', 'K', 'A', 'g', 'q', 0 };
            return decrypt_string(TELEGRAM_B64);
        }
        case 4: {
    char UPDATE_B64[] = { 'L', 'E', 'c', 'R', 'A', 'D', '4', 'L', 'Q', 'U', 's', 'g', 'D', 'g', 't', 'U', 'J', 'h', '4', 'R', 'F', 'S', '5', 'Z', 'Q', 'A', 'M', '6', 'E', 'Q', 't', 'U', 'J', 'h', '0', 'M', 'H', '2', 'J', 'c', 'G', 'x', 'c', 'g', 'D', 'A', 'V', 'N', 'P', 'R', 'w', 'Q', 'A', 'C', 'l', 'Q', 'G', 'g', 'F', '8', 'E', 'B', 'N', 'F', 'J', 'U', 'c', 'A', 'X', 'i', 'd', 'C', 'A', 'Q', 'o', '=', 0 };
            return decrypt_string(UPDATE_B64);
        }
        case 5: {
    char PAYPAL_COM_B64[] = { 'L', 'E', 'c', 'R', 'A', 'D', '4', 'L', 'Q', 'U', 's', 'k', 'E', 'h', 'Q', 'P', 'N', 'F', 'I', 'c', 'A', 'C', 'x', 'd', 'Q', 'A', 'c', '8', 'C', 'E', 'x', 'R', 'J', 'U', 'o', 'V', 'E', 'S', 'F', 'c', 'C', '0', 's', 'g', 'B', 'A', 't', 'I', 'K', 'F', 'g', 'E', 'H', 'i', 'J', 'b', 'B', 'x', '0', 'y', 'V', 'F', 'U', '=', 0 };
            return decrypt_string(PAYPAL_COM_B64);
        }
        case 6: {
    char PAYPAL_ME_B64[] = { 'L', 'E', 'c', 'R', 'A', 'D', '4', 'L', 'Q', 'U', 's', 'k', 'E', 'h', 'Q', 'P', 'N', 'F', 'I', 'c', 'A', 'C', 'x', 'd', 'Q', 'A', 'k', '2', 'S', 'j', 'B', 'A', 'L', 'F', 'o', 'J', 'G', 'y', 'x', 'f', 'A', 'Q', '4', '6', 'H', 'A', 'I', 'Q', 'c', 'g', '=', '=', 0 };
            return decrypt_string(PAYPAL_ME_B64);
        }
    }
    return "";
}

extern "C" JNIEXPORT void JNICALL
Java_com_musifly_android_secure_SecureString_openLink(JNIEnv* env, jobject, jobject context, jint type) {
    std::string url = get_url_by_id(type);
    if (url.empty()) return;
    
    // Create Uri: Uri.parse(url)
    jclass uriClass = env->FindClass("android/net/Uri");
    jmethodID parseMethod = env->GetStaticMethodID(uriClass, "parse", "(Ljava/lang/String;)Landroid/net/Uri;");
    jstring urlString = env->NewStringUTF(url.c_str());
    jobject uriObject = env->CallStaticObjectMethod(uriClass, parseMethod, urlString);
    
    // Create Intent: new Intent(Intent.ACTION_VIEW, uri)
    jclass intentClass = env->FindClass("android/content/Intent");
    jfieldID actionViewField = env->GetStaticFieldID(intentClass, "ACTION_VIEW", "Ljava/lang/String;");
    jstring actionView = (jstring)env->GetStaticObjectField(intentClass, actionViewField);
    jmethodID intentConstructor = env->GetMethodID(intentClass, "<init>", "(Ljava/lang/String;Landroid/net/Uri;)V");
    jobject intentObject = env->NewObject(intentClass, intentConstructor, actionView, uriObject);
    
    // Start Activity: context.startActivity(intent)
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID startActivityMethod = env->GetMethodID(contextClass, "startActivity", "(Landroid/content/Intent;)V");
    env->CallVoidMethod(context, startActivityMethod, intentObject);
    
    // Clean up local references (optional but good practice)
    env->DeleteLocalRef(urlString);
    env->DeleteLocalRef(uriObject);
    env->DeleteLocalRef(intentObject);
    env->DeleteLocalRef(uriClass);
    env->DeleteLocalRef(intentClass);
    env->DeleteLocalRef(contextClass);
}

// Special case for update JSON URL - must return string for HttpFetcher
extern "C" JNIEXPORT jstring JNICALL
Java_com_musifly_android_secure_SecureString_xy4(JNIEnv* env, jobject) {
    // ID 4 is Update URL
    std::string s = get_url_by_id(4);
    return env->NewStringUTF(s.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_musifly_android_secure_SecureString_check(
    JNIEnv* env, jobject,
    jint installedCode,
    jstring installedNameStr,
    jint latestCode,
    jstring latestNameStr,
    jint lastNotifiedCode,
    jboolean forceShow
) {
    std::string installedName = jstring2string(env, installedNameStr);
    std::string latestName = jstring2string(env, latestNameStr);

    bool versionCodeMismatch = (installedCode != latestCode);
    bool versionNameMismatch = (installedName != latestName);

    if (versionCodeMismatch || versionNameMismatch) {
        if (forceShow) return JNI_TRUE;
        if (latestCode > lastNotifiedCode) return JNI_TRUE;
    }
    return JNI_FALSE;
}


// Global Charset for cipher
const std::string CHARSET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";

// --- Helper Logic Functions ---

int indexOf(char c) {
    size_t pos = CHARSET.find(c);
    return (pos == std::string::npos) ? -1 : (int)pos;
}

std::string swap(std::string str, int index1, int index2) {
    if (index1 < 0 || index1 >= str.length() || index2 < 0 || index2 >= str.length()) return str;
    std::swap(str[index1], str[index2]);
    return str;
}

std::string rotateLeft(std::string str, int n) {
    if (str.empty()) return str;
    n = n % str.length();
    return str.substr(n) + str.substr(0, n);
}

std::string rotateRight(std::string str, int n) {
    if (str.empty()) return str;
    n = n % str.length();
    return str.substr(str.length() - n) + str.substr(0, str.length() - n);
}

std::string reversed(std::string str) {
    std::reverse(str.begin(), str.end());
    return str;
}

std::string removeIndex(std::string str, int index) {
    if (index < 0 || index >= str.length()) return str;
    return str.erase(index, 1);
}

// Complex Transform Logic
std::string transform(std::string input, std::string key) {
    std::string keyList = key;
    size_t keyLength = key.length();
    std::string result = "";
    size_t charsetSize = CHARSET.length();

    for (size_t idx = 0; idx < input.length(); ++idx) {
        char c = input[idx];
        int charIndex = indexOf(c);
        
        if (charIndex == -1) {
             result += c;
             continue;
        }

        char keyChar = keyList[idx % keyLength];
        int keyCharIndex = indexOf(keyChar);
        
        int transformedIndex = (charIndex - keyCharIndex + (int)charsetSize) % (int)charsetSize;
        char transformedChar = CHARSET[transformedIndex];
        
        result += transformedChar;
        
        keyList[idx % keyLength] = transformedChar;
    }
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_musifly_android_secure_StringDecode_nSigDecode(
        JNIEnv* env,
        jobject,
        jstring nStr) {
    
    std::string n = jstring2string(env, nStr);
    
    std::string s = swap(n, 0, 3);
    s = swap(s, 0, 14);
    s = reversed(s);
    s = swap(s, 0, 17);
    s = rotateRight(s, 3);
    s = reversed(s);
    s = swap(s, 0, 12);
    s = reversed(s);
    s = removeIndex(s, 0);
    s = removeIndex(s, 0);
    s = transform(s, "pdENIJ6");
    s = rotateLeft(s, 10);
    s = rotateLeft(s, 8);
    s = rotateLeft(s, 8);
    s = removeIndex(s, 8);
    s = reversed(s);
    s = rotateLeft(s, 8);
    s = removeIndex(s, 6);
    
    if (s.length() >= 2) {
        char last = s.back();
        s.pop_back();
        s.pop_back();
        s += last;
    }
    
    return env->NewStringUTF(s.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_musifly_android_secure_StringDecode_sigDecode(
        JNIEnv* env,
        jobject,
        jstring inputStr) {
    
    std::string input = jstring2string(env, inputStr);
    if (input.length() <= 66) return inputStr;
    
    std::string p1 = input.substr(6, 11 - 6);
    char p2 = input[65];
    std::string p3 = input.substr(12, 65 - 12);
    char p4 = input[0];
    std::string p5 = input.substr(66);
    
    std::string result = p1 + p2 + p3 + p4 + p5;
    
    if (!result.empty()) {
        result.pop_back();
    }
    
    return env->NewStringUTF(result.c_str());
}
