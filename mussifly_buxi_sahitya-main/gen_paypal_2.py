
def xor_encrypt(text, key):
    key_len = len(key)
    result = []
    for i in range(len(text)):
        char_code = ord(text[i])
        key_code = ord(key[i % key_len])
        xor_val = char_code ^ key_code
        result.append(xor_val)
    result.append(0)
    return result

key = "MussiSecret"
url1 = "https://www.paypal.com/paypalme/sahilkanojiya16"
url2 = "https://www.paypal.me/Sahilkanojiya16"

enc1 = xor_encrypt(url1, key)
enc2 = xor_encrypt(url2, key)

with open("paypal_bytes.txt", "w", encoding="utf-8") as f:
    f.write(f"// {url1}\n")
    f.write(f"const unsigned char PAYPAL_COM_BYTES[] = {{ {', '.join(map(str, enc1))} }};\n\n")
    f.write(f"// {url2}\n")
    f.write(f"const unsigned char PAYPAL_ME_BYTES[] = {{ {', '.join(map(str, enc2))} }};\n")
