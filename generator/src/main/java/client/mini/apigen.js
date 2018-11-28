var _ = require('./autogen/scripts/underscore');
var request = require('request');
var Mustache = require('./autogen/scripts/mustache');
var FS = require('fs');

var PREFIX = 'sf.b2c.mall.api.';
var ROOT_PATH = 'autogen/';
var API_PATH = 'miniapps/api/';
var SECURITY_LEVEL = ['UserLogin', 'RegisteredDevice', 'None'];
var SECURITY_TYPE = {
  'UserLogin': 'SecurityType.UserLogin',
  'RegisteredDevice': 'SecurityType.RegisteredDevice',
  'None': 'SecurityType.None'
};

var API_MUSTACHE = ROOT_PATH + 'mustache/api.mustache';

var SOURCE = [
  // {
  //   name: 'liulian',
  //   src: 'http://115.28.11.229/info.api?json',
  //   filename: 'liulian.json',
  //   filterGroup: ['lluser', 'llorder',  'llproduct']
  // },
  {
    name: 'haitao',
    src: 'http://118.190.22.209/info.api?json',
    filename: 'haitao.json',
    filterGroup: [
      'user',
      'wowUser',
      'product',
      'wowProduct',
      'wowStore',
      'wowActivity',
      'order',
      'wowOrder',
      'shopcart',
      'minicart',
      'coupon',
      'wowCoupon',
      'groupPromotion',
      'integral',
      'b2cmall',
      'Integral',
      'minicart',
      'search',
      'wowShopcart',
      'wowCoupon',
      'wowPayment',
      'wowContainer',
      'wow3prefacing',

      'wowGroupon',
    ],

    fileFilter: [

      'WowActivity_GetItemActivityInfo',
      'WowActivity_GetSimpleActivityInfoByStoreId',

      // 优惠券
      'WowCoupon_GetUserCouponList',
      'WowCoupon_GetCpCount',
      'WowCoupon_GetUserCouponNum',
      'WowCoupon_ReceiveCoupon',
      'WowCoupon_ReceiveExCode',
      'WowCoupon_GetCouponDetail',
      'Coupon_GetCpCount',

      // 商品
      'WowProduct_GetItemInfo',
      'WowProduct_GetProductHotData',
      'WowProduct_GetSimpleGrouponItemActivityInfo',

      //拼团活动
      'WowGrouponActivity_GetCurrentGrouponInfo',
      'WowGrouponActivity_CreateGrouponValidation',
      'WowGrouponActivity_GetUserItemGroupInfo',
      'WowGrouponActivity_JoinGrouponValidation',


      //订单
      'WowOrder_ConfirmReceive',
      'WowOrder_GetOrderList',
      'Order_SubmitOrderForAllSysV2',
      'WowOrder_RequestPay',
      'WowOrder_GetPayCompleteResultV2',
      'WowOrder_OrderPriceReCalculate',
      // 'WowOrder_OrderRender',
      // 'Order_GetOrderList',
      // 'Order_DeleteOrder',
      // 'WowOrder_DeleteOrder',
      // 'Order_CancelOrder',
      // 'Order_ConfirmReceive',
      // 'WowOrder_ConfirmReceive',
      // 'Order_GetOrderV2',
      // 'WowOrder_GetOrder',
      // 'Order_QueryOrderStatusAmount',
      // 'Order_OrderRender',
      // 'Order_SubmitOrderForAllSysV2',
      // 'Order_OrderPriceReCalculate',
      // 'WowOrder_SubmitOrderForAllSys',
      // 'WowOrder_OrderPriceReCalculate',
      // 'WowOrder_GetPayCompleteResult',
      'WowOrder_QueryOrderStatusAmount',

      // 店铺
      'WowStore_GetStoreInfoList',
      'WowStore_GetSimpleStoreById',

      // 用户登录
      'User_RenewToken',
      'User_DeviceRegister',
      'User_SingleSignOn',
      'User_RecordUserInfo',
      'User_GetUserInfo',
      'WowUser_PartnerAppLogin',
      'WowUser_GetPhoneNumberToBindWxV2',
      'WowUser_PartnerAppBindV2',
      'User_GetRecAddressList',
      'User_GetRecvInfo',
      'User_DownSmsCode'
    ]
  }
];

function createJSON(grunt, done) {
  var count = 0;
  _.each(SOURCE, function (source, key, list) {
    var setting = {
      method: 'GET',
      url: source.src
    };

    request(setting, function (error, response, body) {
      count++;
      if (error) {
        return console.log(error);
      } else {
        console.log('从服务端获得json索引文件:' + source.filename);

        var path = ROOT_PATH + '/source/';
        if (FS.existsSync(path)) {
          console.log('已经创建过此更新目录了');
        } else {
          FS.mkdirSync(path);
          console.log('更新目录已创建成功\n');
        }

        var jsonFile = FS.createWriteStream(path + source.filename, {
          flags: 'w',
          defaultEncoding: 'utf8'
        });
        jsonFile.write(body);
        jsonFile.end();

        if (count == SOURCE.length) {
          setTimeout(function () {
            createAPI();
          }, 1000);

        }
      }
    });
  });
}

function createAPI() {
  var apiTpl = FS.readFileSync(API_MUSTACHE, {
    encoding: 'utf8'
  });

  FS.readdir(ROOT_PATH + '/source/', function (err, files) {
    _.each(files, function (file) {
      if (file === ".DS_Store") {
        return;
      }

      console.log('开始解析索引文件:' + file);

      var data = FS.readFileSync(ROOT_PATH + '/source/' + file, {
        encoding: 'utf8'
      });
      var found = _.findWhere(SOURCE, {
        filename: file
      });
      // console.log('开始解析索引文件:' + data);
      var json = JSON.parse(data);

      for (var i in json.apiList) {

        var it = json.apiList[i];
        if (it.parameterInfoList) {
          var info = it.parameterInfoList;
          if (_.isArray(info)) {
            for (var i = 0; i < info.length; i++) {
              if (i == info.length - 1) {
                info[i].last = true;
              }

              if (info[i].type.toLowerCase().indexOf('api') > -1) {
                info[i].type = 'json';
              }
            }
          } else {
            if (info.type.toLowerCase().indexOf('api') > -1) {
              info.type = 'json';
            }
            info.last = true;
          }
        }

        if (it.errorCodeList) {
          var error = it.errorCodeList;

          if (_.isArray(error) && error.length > 0) {
            error[error.length - 1].last = true;
          } else {
            error.last = true;
          }
        }

        var array = it.methodName.split(".");

        if (array.length > 1) {
          var first = array[0];
          var second = array[1];

          it.className = first.substring(0, 1).toUpperCase() + first.substring(1, first.length) + "_" + second.substring(0, 1).toUpperCase() + second.substring(1, second.length);
        }

        if (found.fileFilter.indexOf(it.className) > -1 && found.filterGroup.indexOf(it.groupName) > -1 && SECURITY_LEVEL.indexOf(it.securityLevel) > -1) {
          it.securityType = SECURITY_TYPE[it.securityLevel];
          var fileContent = Mustache.render(apiTpl, it);

          var jsonFile = FS.createWriteStream(API_PATH + it.className + '.js', {
            flags: 'w',
            defaultEncoding: 'utf8'
          });
          jsonFile.write(fileContent);
          jsonFile.end();
        }
      }
    });
  });
}

/**
 * @description 从不同的source中获得所有的API接口
 */
function autogen() {
  createJSON()
}

autogen();
