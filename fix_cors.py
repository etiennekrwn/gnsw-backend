# -*- coding: utf-8 -*-
import sys
p = r'c:\Users\HP\Documents\GitHub\GNSW\gnsw-backend\src\main\java\com\gnsw\gnsw_backend\config\CorsConfig.java'
with open(p, 'r', encoding='utf-8') as f:
    c = f.read()

old = '''        // Allow all origins for the temporary Railway host.
        // allowedOriginPatterns("*") works together with allowCredentials(true),
        // unlike setAllowedOrigins(List.of("*")) which Spring rejects with credentials.
        config.setAllowedOriginPatterns(List.of("*"));
'''
new = '''        // Restrict CORS to the three GNSW frontends (configured via env on Railway).
        // allowedOriginPatterns(...) works with allowCredentials(true), unlike setAllowedOrigins.
        config.setAllowedOriginPatterns(List.of(mainSiteUrl, adminUrl, memberPortalUrl));
'''
n = c.count(old)
if n != 1:
    print('FATAL count =', n); sys.exit(1)
c = c.replace(old, new, 1)
c = c.replace('\r\n', '\n').replace('\n', '\r\n')
with open(p, 'w', encoding='utf-8', newline='') as f:
    f.write(c)
print('OK CorsConfig')
