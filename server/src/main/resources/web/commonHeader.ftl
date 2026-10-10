<#setting classic_compatible=true>
<link rel="icon" href="./favicon.ico" type="image/x-icon">
<script src="js/watermark.js" type="text/javascript"></script>

<script>
    /**
     * 初始化水印
     */
    function initWaterMark() {
        let watermarkTxt = '${watermarkTxt}';
        if (watermarkTxt === '') {
            return;
        }
        let lastWidth = 0;
        let lastHeight = 0;
        const checkResize = () => {
            const currentWidth = document.documentElement.scrollWidth;
            const currentHeight = document.documentElement.scrollHeight;
            // 检测尺寸是否变化
            if (currentWidth === lastWidth && currentHeight === lastHeight) {
                return;
            }
            // 如果变化了, 重新初始化水印
            watermark.init({
                watermark_txt: '${watermarkTxt}',
                watermark_x: 0,
                watermark_y: 0,
                watermark_rows: 0,
                watermark_cols: 0,
                watermark_x_space: ${watermarkXSpace},
                watermark_y_space: ${watermarkYSpace},
                watermark_font: '${watermarkFont}',
                watermark_fontsize: '${watermarkFontsize}',
                watermark_color: '${watermarkColor}',
                watermark_alpha: ${watermarkAlpha},
                watermark_width: ${watermarkWidth},
                watermark_height: ${watermarkHeight},
                watermark_angle: ${watermarkAngle},
            });
            // 更新存储的宽口大小
            lastWidth = currentWidth;
            lastHeight = currentHeight;
        };
        setInterval(checkResize, 1000);
    }
</script>

<style>
    * {
        margin: 0;
        padding: 0;
    }

    html, body {
        height: 100%;
        width: 100%;
    }
</style>
