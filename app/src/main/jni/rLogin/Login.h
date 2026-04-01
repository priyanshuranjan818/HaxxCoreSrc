#ifndef LOGIN_H
#define LOGIN_H

#include "StrEnc.h"
#include "Includes.h"
#include "curl/curl.h"
#include "json.hpp"
#include "Log.h"
#include <jni.h>
#include <string>
#include "oxorany.h"
#include <jni.h>
#include <string>
#include <android/log.h>
#include <openssl/evp.h>
#include <openssl/pem.h>
#include <openssl/rsa.h>
#include <openssl/err.h>
#include <openssl/aes.h>
#include <openssl/md5.h>
using json = nlohmann::ordered_json;
bool xConnected = true, xServerConnection = true, memek = true;
std::string g_Auth, g_Token,ts,device;
bool check;

static std::string exdate;


const char *GetAndroidID(JNIEnv *env, jobject context) {
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID getContentResolverMethod = env->GetMethodID(contextClass,"getContentResolver","()Landroid/content/ContentResolver;");
    jclass settingSecureClass = env->FindClass("android/provider/Settings$Secure");
    jmethodID getStringMethod = env->GetStaticMethodID(settingSecureClass,"getString", "(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;");

    auto obj = env->CallObjectMethod(context, getContentResolverMethod);
    auto str = (jstring) env->CallStaticObjectMethod(settingSecureClass, getStringMethod, obj,env->NewStringUTF("android_id"));
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceModel(JNIEnv *env) {
    jclass buildClass = env->FindClass("android/os/Build");
    jfieldID modelId = env->GetStaticFieldID(buildClass, "MODEL","Ljava/lang/String;");

    auto str = (jstring) env->GetStaticObjectField(buildClass, modelId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceBrand(JNIEnv *env) {
    jclass buildClass = env->FindClass("android/os/Build");
    jfieldID modelId = env->GetStaticFieldID(buildClass, "BRAND","Ljava/lang/String;");

    auto str = (jstring) env->GetStaticObjectField(buildClass, modelId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetPackageName(JNIEnv *env, jobject context) {
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID getPackageNameId = env->GetMethodID(contextClass, "getPackageName","()Ljava/lang/String;");

    auto str = (jstring) env->CallObjectMethod(context, getPackageNameId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceUniqueIdentifier(JNIEnv *env, const char *uuid) {
    jclass uuidClass = env->FindClass("java/util/UUID");

    auto len = strlen(uuid);

    jbyteArray myJByteArray = env->NewByteArray(len);
    env->SetByteArrayRegion(myJByteArray, 0, len, (jbyte *) uuid);

    jmethodID nameUUIDFromBytesMethod = env->GetStaticMethodID(uuidClass,"nameUUIDFromBytes","([B)Ljava/util/UUID;");
    jmethodID toStringMethod = env->GetMethodID(uuidClass, "toString","()Ljava/lang/String;");

    auto obj = env->CallStaticObjectMethod(uuidClass, nameUUIDFromBytesMethod, myJByteArray);
    auto str = (jstring) env->CallObjectMethod(obj, toStringMethod);
    return env->GetStringUTFChars(str, 0);
}

struct MemoryStruct {
    char *memory;
    size_t size;
};

static size_t WriteMemoryCallback(void *contents, size_t size, size_t nmemb, void *userp) {
    size_t realsize = size * nmemb;
    struct MemoryStruct *mem = (struct MemoryStruct *) userp;

    mem->memory = (char *) realloc(mem->memory, mem->size + realsize + 1);
    if (mem->memory == NULL) {
        return 0;
    }

    memcpy(&(mem->memory[mem->size]), contents, realsize);
    mem->size += realsize;
    mem->memory[mem->size] = 0;

    return realsize;
}

std::string CalcMD5(std::string s) {
    std::string result;

    unsigned char hash[MD5_DIGEST_LENGTH];
    char tmp[4];

    MD5_CTX md5;
    MD5_Init(&md5);
    MD5_Update(&md5, s.c_str(), s.length());
    MD5_Final(hash, &md5);
    for (unsigned char i : hash) {
        sprintf(tmp, "%02x", i);
        result += tmp;
    }
    return result;
}

std::string CalcSHA256(std::string s) {
    std::string result;

    unsigned char hash[SHA256_DIGEST_LENGTH];
    char tmp[4];

    SHA256_CTX sha256;
    SHA256_Init(&sha256);
    SHA256_Update(&sha256, s.c_str(), s.length());
    SHA256_Final(hash, &sha256);
    for (unsigned char i : hash) {
        sprintf(tmp, "%02x", i);
        result += tmp;
    }
    return result;

}

std::string FetchText(const std::string &url) {
    CURL *curl = curl_easy_init();
    if (!curl) {
        return "Error";
    }

    CURLcode res;
    struct MemoryStruct chunk;
    chunk.memory = (char *) malloc(1);
    chunk.size = 0;

    if (curl) {
        curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *) &chunk);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0L);
        curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "POST");
        curl_easy_setopt(curl, CURLOPT_USERAGENT, "");

        res = curl_easy_perform(curl);
        if (res != CURLE_OK) {

            curl_easy_cleanup(curl);
            free(chunk.memory);
            return "Error";
        }

        std::string text(chunk.memory);
        curl_easy_cleanup(curl);
        free(chunk.memory);
        return text;
    }

    return "Error";
}
extern "C" JNIEXPORT jstring JNICALL native_Check(JNIEnv *env, jclass clazz, jobject mContext, jstring mUserKey) {
    const char* user_key = env->GetStringUTFChars(mUserKey, 0);
    std::string hwid = user_key;
    hwid += GetAndroidID(env, mContext);StrEnc("lakyWbF&oaI*n86C`nK<mwLp*0EeCIO=Zvaw", "\x04\x15\x1F\x09\x24\x58\x69\x09\x02\x0E\x2D\x04\x1E\x59\x58\x26\x0C\x1D\x3F\x53\x1F\x12\x62\x03\x5A\x51\x26\x00\x6C\x2A\x20\x53\x34\x13\x02\x03", 36).c_str();/*https://mod.panelstore.space/connect*/ StrEnc("lakyWbF&oaI*n86C`nK<mwLp*0EeCIO=Zvaw", "\x04\x15\x1F\x09\x24\x58\x69\x09\x02\x0E\x2D\x04\x1E\x59\x58\x26\x0C\x1D\x3F\x53\x1F\x12\x62\x03\x5A\x51\x26\x00\x6C\x2A\x20\x53\x34\x13\x02\x03", 36).c_str();/*https://mod.panelstore.space/connect*/ StrEnc("lakyWbF&oaI*n86C`nK<mwLp*0EeCIO=Zvaw", "\x04\x15\x1F\x09\x24\x58\x69\x09\x02\x0E\x2D\x04\x1E\x59\x58\x26\x0C\x1D\x3F\x53\x1F\x12\x62\x03\x5A\x51\x26\x00\x6C\x2A\x20\x53\x34\x13\x02\x03", 36).c_str();/*https://mod.panelstore.space/connect*/ StrEnc("lakyWbF&oaI*n86C`nK<mwLp*0EeCIO=Zvaw", "\x04\x15\x1F\x09\x24\x58\x69\x09\x02\x0E\x2D\x04\x1E\x59\x58\x26\x0C\x1D\x3F\x53\x1F\x12\x62\x03\x5A\x51\x26\x00\x6C\x2A\x20\x53\x34\x13\x02\x03", 36).c_str();
    hwid += GetDeviceModel(env);
    hwid += GetDeviceBrand(env);
    std::string UUID = GetDeviceUniqueIdentifier(env, hwid.c_str());
    std::string errMsg;
    struct MemoryStruct chunk{};
    chunk.memory = (char *) malloc(1);
    chunk.size = 0;
    CURL *curl;
    CURLcode res;
    curl = curl_easy_init();
    if (curl) {
        char lol[1000];
        sprintf(lol,oxorany("https://kalajadu.vippanel.shop/connect"));
        
        curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, oxorany("POST"));
        curl_easy_setopt(curl, CURLOPT_URL, lol);+
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1);
        curl_easy_setopt(curl, CURLOPT_DEFAULT_PROTOCOL, oxorany("https"));
        struct curl_slist *headers = NULL;
        headers = curl_slist_append(headers, oxorany("Accept: application/json"));
        headers = curl_slist_append(headers, oxorany("Content-Type: application/x-www-form-urlencoded"));
        headers = curl_slist_append(headers, oxorany("Charset: UTF-8"));
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        char data[4096];
        sprintf(data, oxorany("game=PUBG&user_key=%s&serial=%s"), user_key, UUID.c_str());
        curl_easy_setopt(curl, CURLOPT_POST, 1);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, data);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *) &chunk);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYSTATUS, 0);
        curl_easy_setopt(curl, CURLOPT_USERAGENT, oxorany(""));
        res = curl_easy_perform(curl);
        if (res == CURLE_OK) {
            try {
                json result = json::parse(chunk.memory);
                auto STATUS = std::string{"status"};
                if (result[STATUS] == true) {
                    auto DATA = std::string{"data"};
                    auto TOKEN = std::string{"token"};
                    auto RNG = std::string{"rng"};
                    exdate = result["data"]["EXP"].get<std::string>();
                    std::string token = result[DATA][TOKEN].get<std::string>();
                    time_t rng = result[DATA][RNG].get<time_t>();
                    if (rng + 30 > time(0)) {
                        std::string auth = oxorany("PUBG");
                        auth += "-";
                        auth += user_key;
                        auth += "-";
                        auth += UUID;
                        auth += "-";
                      	std::string license = oxorany("Vm8Lk7Uj2JmsjCPVPVjrLa7zgfx3uz9E");
                        auth += license.c_str();
                        std::string outputAuth = CalcMD5(auth);
                        g_Token = token;
                        g_Auth = outputAuth;
                        xConnected = true; // Bypassed check
                        xServerConnection = true;
                        memek = true;
                        if (xConnected && xServerConnection) {
                            printf(oxorany("Login Success \n"));
                        }
                    }
                } else {
                    auto REASON = std::string{"reason"};
                    errMsg = result[REASON].get<std::string>();
                }
            } catch (json::exception &e) {
                errMsg = e.what();
            }
        } else {
            errMsg = curl_easy_strerror(res);
        }
    }
    curl_easy_cleanup(curl);
    return xConnected ? env->NewStringUTF(StrEnc("8q", "\x77\x3A", 2).c_str()) : env->NewStringUTF(errMsg.c_str());
}
#endif

