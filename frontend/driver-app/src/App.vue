<script>
export default {
  onLaunch() {
    console.log('青途司机端启动')
  }
}
</script>

<style>
page {
  background-color: #f4f6fa;
  color: #303133;
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

.page {
  padding: 24rpx;
  min-height: 100vh;
}

.card {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 6rpx 20rpx rgba(31, 45, 61, 0.05);
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2d3d;
  margin-bottom: 20rpx;
}

.muted {
  color: #909399;
}

button::after {
  border: none;
}
</style>
