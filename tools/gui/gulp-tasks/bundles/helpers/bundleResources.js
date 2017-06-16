export default function(prefix, fileArray) {
    return fileArray.map((file) => {
        return prefix + file;
    });
}