<script>
export default {
  onLaunch() {},
  onShow() {},
  onHide() {}
}
</script>

<style>
page {
  background-color: #f5f7fb;
  color: #1f2733;
  font-size: 28rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', 'PingFang SC',
    'Microsoft YaHei', sans-serif;
}

view,
text,
button,
input {
  box-sizing: border-box;
}

button::after {
  border: none;
}

.qt-page {
  min-height: 100vh;
  padding: 24rpx;
}

.qt-card {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 8rpx 28rpx rgba(139, 163, 199, 0.06);
}

.qt-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #ffffff;
  background-color: #8BA3C7;
  border: none;
  text-align: center;
}

.qt-btn[disabled] {
  color: #ffffff !important;
  background-color: #b9c4d8 !important;
}

.qt-btn-light {
  width: 100%;
  height: 76rpx;
  line-height: 76rpx;
  margin: 0;
  border-radius: 38rpx;
  font-size: 28rpx;
  font-weight: 500;
  color: #8BA3C7;
  background-color: #EDF1F7;
  border: none;
  text-align: center;
}

.qt-empty {
  padding: 120rpx 40rpx;
  text-align: center;
  color: #9aa3b2;
  font-size: 26rpx;
}

.qt-empty-title {
  display: block;
  margin-top: 16rpx;
  color: #5f6b80;
  font-size: 30rpx;
}

.qt-tag {
  display: inline-block;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  line-height: 32rpx;
}

.qt-tag-blue {
  color: #8BA3C7;
  background-color: #EDF1F7;
}

.qt-tag-green {
  color: #16a34a;
  background-color: #e8f8ef;
}

.qt-tag-orange {
  color: #d97706;
  background-color: #fff4e5;
}

.qt-tag-gray {
  color: #8a93a6;
  background-color: #f0f2f6;
}

.qt-tag-red {
  color: #dc2626;
  background-color: #fdecec;
}
</style>
