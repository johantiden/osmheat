
# Instructions:
# 1. Start the server in a separate terminal `mvn clean compile exec:java`
# 2. Run this command an new terminal
# 3. Inspect the output files, either `overlay.png` or the blended `result.png`

curl 'https://tile.openstreetmap.org/16/32757/21834.png' -H 'Accept: image/png' --output tile.png
curl 'http://localhost:8080/16/32757/21834.png' -H 'Accept: image/png' --output overlay.png

# Assumes you have imagemagick installed, otherwise just put the above images on top of each other manually!
convert tile.png overlay.png -composite result.png
