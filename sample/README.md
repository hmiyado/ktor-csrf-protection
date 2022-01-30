# ktor-csrf-protection sample application

```sh
$ ./gradlew run
# Server runs on http://0.0.0.0:8080

$ curl -v -X POST http://0.0.0.0:8080/protected                                                                                                                                                                                                      Sun Jan 30 18:00:52 2022
*   Trying 0.0.0.0:8080...
* Connected to 0.0.0.0 (127.0.0.1) port 8080 (#0)
> POST /protected HTTP/1.1
> Host: 0.0.0.0:8080
> User-Agent: curl/7.77.0
> Accept: */*
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 403 Forbidden
< Set-Cookie: client_session=8ac68139b4ff8b193ee1bdf3bd8e3f89; Max-Age=604800; Expires=Sun, 06 Feb 2022 09:09:25 GMT; Path=/; HttpOnly; $x-enc=URI_ENCODING
< Content-Length: 0
< 
* Connection #0 to host 0.0.0.0 left intact

# use client_session acquired the above request
$ curl -v -X POST -H "Cookie: client_session=8ac68139b4ff8b193ee1bdf3bd8e3f89;" http://0.0.0.0:8080/protected
*   Trying 0.0.0.0:8080...
* Connected to 0.0.0.0 (127.0.0.1) port 8080 (#0)
> POST /protected HTTP/1.1
> Host: 0.0.0.0:8080
> User-Agent: curl/7.77.0
> Accept: */*
> Cookie: client_session=8ac68139b4ff8b193ee1bdf3bd8e3f89;
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 403 Forbidden
< Set-Cookie: client_session=8ac68139b4ff8b193ee1bdf3bd8e3f89; Max-Age=604800; Expires=Sun, 06 Feb 2022 09:10:13 GMT; Path=/; HttpOnly; $x-enc=URI_ENCODING
< X-CSRF-TOKEN: 64b7fdd397bfd5dd2af0c0f8a76a7cb5
< Content-Length: 0
< 
* Connection #0 to host 0.0.0.0 left intact

# use client_session and X-CSRF-TOKEN acquired the above request
$ curl -v -X POST -H "Cookie: client_session=8ac68139b4ff8b193ee1bdf3bd8e3f89;" -H "X-CSRF-TOKEN: 64b7fdd397bfd5dd2af0c0f8a76a7cb5" http://0.0.0.0:8080/protected
*   Trying 0.0.0.0:8080...
* Connected to 0.0.0.0 (127.0.0.1) port 8080 (#0)
> POST /protected HTTP/1.1
> Host: 0.0.0.0:8080
> User-Agent: curl/7.77.0
> Accept: */*
> Cookie: client_session=8ac68139b4ff8b193ee1bdf3bd8e3f89;
> X-CSRF-TOKEN: 64b7fdd397bfd5dd2af0c0f8a76a7cb5
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Set-Cookie: client_session=8ac68139b4ff8b193ee1bdf3bd8e3f89; Max-Age=604800; Expires=Sun, 06 Feb 2022 09:21:48 GMT; Path=/; HttpOnly; $x-enc=URI_ENCODING
< X-CSRF-TOKEN: 64b7fdd397bfd5dd2af0c0f8a76a7cb5
< Content-Length: 0
< 
* Connection #0 to host 0.0.0.0 left intact
```
