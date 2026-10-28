import base64

def xor_cipher(text, key):
    return bytearray([b ^ key[i % len(key)] for i, b in enumerate(text.encode('utf-8'))])

def to_stack_string(name, data):
    # data is the Base64 string
    chars = [f"'{c}'" for c in data]
    chars.append('0') # Null terminator
    return f"    char {name}[] = {{ {', '.join(chars)} }};"

def generate_cpp_code():
    key_str = "D3epM1ndSec!"
    key_bytes = key_str.encode('utf-8')
    
    targets = [
        ("INSTA_B64", "https://instagram.com/sahitya_000"),
        ("INSTA_WWW_B64", "https://www.instagram.com/sahitya_000"),
        ("TELEGRAM_B64", "https://t.me/MussiFly"),
        ("UPDATE_B64", "https://skhub-tech.github.io/mussifly/update/update.json"),
        ("PAYPAL_COM_B64", "https://www.paypal.com/paypalme/sahilkanojiya16"),
        ("PAYPAL_ME_B64", "https://www.paypal.me/Sahilkanojiya16")
    ]
    
    print("// Generated Stack Strings")
    print("#include <jni.h>")
    print("#include <string>")
    print("#include <vector>")
    print("#include <android/log.h>")
    print("#include <unistd.h>")
    print("")
    
    # Helper functions reused
    print("""
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
""")

    print("std::string get_url_by_id(int id) {")
    print("    switch(id) {")
    
    for i, (name, url) in enumerate(targets):
        # 1. XOR
        xored = xor_cipher(url, key_bytes)
        # 2. Base64
        b64 = base64.b64encode(xored).decode('utf-8')
        
        print(f"        case {i+1}: {{")
        print(to_stack_string(name, b64))
        print(f"            return decrypt_string({name});")
        print("        }")

    print("    }")
    print('    return "";')
    print("}")
    
    print("""
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

// Helper to convert jstring to std::string
std::string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr) return "";
    const char *chars = env->GetStringUTFChars(jStr, nullptr);
    std::string ret(chars);
    env->ReleaseStringUTFChars(jStr, chars);
    return ret;
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
""")

if __name__ == "__main__":
    generate_cpp_code()
