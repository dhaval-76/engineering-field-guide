# DNS: what does a name resolve to, and for how long is it cached?
dig +noall +answer shop.example.com           # A records with remaining TTL
dig +trace shop.example.com                    # walk root → TLD → authoritative

# Where does the time go in one request?
curl -o /dev/null -s -w '
  dns:        %{time_namelookup}s
  tcp:        %{time_connect}s
  tls:        %{time_appconnect}s
  first byte: %{time_starttransfer}s
  total:      %{time_total}s
  http:       %{http_version}
' https://shop.example.com/cart

# Which protocol versions does the server speak?
curl -sI --http2 https://shop.example.com | head -1
curl -sI --http3 https://shop.example.com | head -1    # needs a curl built with HTTP/3

# Inspect the certificate chain and expiry
openssl s_client -connect shop.example.com:443 -servername shop.example.com </dev/null 2>/dev/null \
  | openssl x509 -noout -subject -issuer -dates

# Connections on this machine: established, TIME_WAIT counts, listening ports
ss -tan state established | wc -l
ss -tan state time-wait | wc -l
ss -tlnp
