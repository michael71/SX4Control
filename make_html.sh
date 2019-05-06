# see https://www.npmjs.com/package/markdown-folder-to-html

markdown-folder-to-html rawdocs
rm -rf docs
mv _rawdocs docs

echo "index.html manuell kopieren in den assets/docs folder und nachbearbeiten, da internet hyperlinks im webview nicht funktionieren und Optik nicht schön aussieht" 
