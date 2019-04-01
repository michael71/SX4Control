# see https://www.npmjs.com/package/markdown-folder-to-html

markdown-folder-to-html rawdocs
rm -rf docs
mv _rawdocs docs
rm app/src/main/res/docs/*
cp docs/* app/src/main/assets/docs
 
