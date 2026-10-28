# Secure String Management

This project uses a custom system to obfuscate sensitive strings (like API URLs) in the native library.

## How to Update Strings

1.  **Open the file:** `app/secret_strings.json`
2.  **Edit the values:** Change the URLs or strings as needed.
    ```json
    {
      "strings": {
        "LINK_INSTA_APP": "https://instagram.com/new_username",
        ...
      }
    }
    ```
3.  **Build the App:**
    *   Simply run the app or build it (`Build > Make Project`).
    *   The build system automatically runs `generate_secrets.py`.
    *   This script:
        *   Generates a **new random encryption key**.
        *   Encrypts your new strings.
        *   Updates `app/src/main/cpp/native-lib.cpp`.

## Troubleshooting

*   **File not visible?** Switch Android Studio view from **Android** to **Project**. The file is directly inside the `app/` folder.
*   **Strings not updating?** Make sure Python is installed and added to your system PATH.
