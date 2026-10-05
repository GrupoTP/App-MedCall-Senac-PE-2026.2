import zipfile

browser = r"C:\Users\polya/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin-wrappers/kotlin-browser-js/2026.8.5/2a53c952da62033e9b73ea56626b15d7f0492d51/kotlin-browser-js-2026.8.5.klib"
z = zipfile.ZipFile(browser)
data = z.read("default/ir/strings.knt")

def around(key, after=700):
    i = data.find(key.encode())
    print("====", key, i)
    if i < 0:
        return
    chunk = data[i:i+after]
    print("".join(chr(b) if 32 <= b < 127 else "\n" for b in chunk))
    print()

around("web/serviceworker/ServiceWorkerGlobalScope.kt", 500)
around("IDBFactoryDEFINED", 600)
around("webcacheCacheDEFINED", 500)
around("FetchEventDEFINED", 400)
around("ExtendableEventDEFINED", 300)
around("installEvent", 200)
