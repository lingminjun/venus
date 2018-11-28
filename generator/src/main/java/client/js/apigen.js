var _ = require('underscore');
var request = require('request');
var Mustache = require('mustache');

var PREFIX = 'sf.b2c.mall.api.';
var ROOT_PATH = 'autogen/';
var FILE_PATH = 'app/scripts/';
var SECURITY_LEVEL = ['UserLogin', 'RegisteredDevice', 'None'];
var SECURITY_TYPE = {
  'UserLogin': 'SecurityType.UserLogin',
  'RegisteredDevice': 'SecurityType.RegisteredDevice',
  'None': 'SecurityType.None'
};

var API_MUSTACHE = ROOT_PATH + '/mustache/api.mustache';

var SOURCE = [
  // {
  //   name: 'liulian',
  //   src: 'http://115.28.11.229/info.api?json',
  //   filename: 'liulian.json',
  //   filterGroup: []
  // },
  {
    name: 'haitao',
    src: 'http://115.28.160.84/info.api?json',
    filename: 'haitao.json',
    filterGroup: ['integral','order', 'minicart', 'user', 'logistics', 'product', 'shopcart', 'b2cmall', 'sc', 'supplychain', 'coupon', 'cp', 'promotion', 'payment','categoryPage','finance'],
    fileFilter: [
      'sf.b2c.mall.api.b2cmall.getProductHotDataList',
      'sf.b2c.mall.api.coupon.receiveCoupon',
      'sf.b2c.mall.api.user.partnerLogin',
      'sf.b2c.mall.api.user.singleSignOn',
      'sf.b2c.mall.api.coupon.getMemberGradeCouponList'
    ]
  }
];

function createJSON(grunt, done) {
  var count = 0;

  _.each(SOURCE, function(source, key, list) {
    var setting = {
      method: 'GET',
      url: source.src,
      gzip: true
    };

    request(setting, function(error, response, body) {
      count++;
      if (error) {
        return grunt.log.error(error);
      } else {
        grunt.log.ok('从服务端获得json索引文件:' + source.filename);
        grunt.file.write(ROOT_PATH + '/source/' + source.filename, body);
      }

      if (count == SOURCE.length) {
        createAPI(grunt, done);
      }
    });
  });
}

function createAPI(grunt, done) {
  var apiTpl = grunt.file.read(API_MUSTACHE, {
    encoding: 'utf8'
  });
  grunt.file.recurse(ROOT_PATH + '/source/', function(abspath, rootdir, subdir, file) {

    grunt.log.ok('开始解析索引文件:' + file);

    var data = grunt.file.read(ROOT_PATH + '/source/' + file, {
      encoding: 'utf8'
    });
    var found = _.findWhere(SOURCE, {
      filename: file
    });
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

        if (_.isArray(error)) {
          error[error.length - 1].last = true;
        } else {
          error.last = true;
        }
      }

      if (found.fileFilter.indexOf("sf.b2c.mall.api." + it.methodName) > -1 && found.filterGroup.indexOf(it.groupName) > -1 && SECURITY_LEVEL.indexOf(it.securityLevel) > -1) {
        it.securityType = SECURITY_TYPE[it.securityLevel];
        var fileContent = Mustache.render(apiTpl, it);
        grunt.file.write(FILE_PATH + '/api/' + PREFIX + it.methodName + '.js', fileContent);
      }
    }
  });

  done();
}

/**
 * @description 从不同的source中获得所有的API接口
 */
function autogen(grunt, done) {
  createJSON(grunt, done)
}

exports.autogen = autogen;