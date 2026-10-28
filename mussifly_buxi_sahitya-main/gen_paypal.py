
def xor_encrypt(text, key):
    key_len = len(key)
    result = []
    for i in range(len(text)):
        char_code = ord(text[i])
        key_code = ord(key[i % key_len])
        xor_val = char_code ^ key_code
        result.append(xor_val)
    # Add null terminator as 0 (not XORed, just 0)
    result.append(0)
    return result

key = "MussiSecret"
url1 = "https://www.paypal.com/paypalme/sahilkanojiya16"
url2 = "https://www.paypal.me/Sahilkanojiya16"

enc1 = xor_encrypt(url1, key)
enc2 = xor_encrypt(url2, key)

print(f"// {url1}")
print(f"const unsigned char PAYPAL_COM_BYTES[] = {{ {', '.join(map(str, enc1))} }};")

print(f"// {url2}")
print(f"const unsigned char PAYPAL_ME_BYTES[] = {{ {', '.join(map(str, enc2))} }};")
