// LuckyExcel's bundled getBinaryContent reads window.XMLHttpRequest.
// Web Worker exposes XMLHttpRequest on self, so provide a minimal window alias
// before loading the UMD bundle.
self.window = self;

importScripts('./luckyexcel.umd.js');

self.console.log = function () {};

self.onmessage = function (event) {
    var data = event.data || {};
    var url = data.url;
    var name = data.name;

    if (!url) {
        self.postMessage({
            type: 'error',
            message: '文件URL为空'
        });
        return;
    }

    try {
        LuckyExcel.transformExcelToLuckyByUrl(
            url,
            name,
            function (exportJson, luckysheetfile) {
                if (!exportJson || !exportJson.sheets || exportJson.sheets.length === 0) {
                    self.postMessage({
                        type: 'error',
                        message: '读取excel文件内容失败!'
                    });
                    return;
                }

                self.postMessage({
                    type: 'success',
                    exportJson: exportJson
                });
            },
            function (error) {
                self.postMessage({
                    type: 'error',
                    message: error && error.message ? error.message : String(error)
                });
            }
        );
    } catch (error) {
        self.postMessage({
            type: 'error',
            message: error && error.message ? error.message : String(error)
        });
    }
};
