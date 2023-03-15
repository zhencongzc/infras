### 所有分行的告警kpi指标

GET http://localhost:8085/infras/workBench/alarmKPI?type=week

- 返回值示例
```json
{
  "state": 200,
  "message": "操作成功！",
  "data": [
    {
      "kpiName": "紧急告警个数",
      "kpiValue": "38964",
      "kpiTrend": "6870",
      "kpiTrendState": "1"
    },
    {
      "kpiName": "紧急告警响应率",
      "kpiValue": "125.00%",
      "kpiTrend": "-57%",
      "kpiTrendState": "0"
    },
    {
      "kpiName": "其它告警",
      "kpiValue": "1584",
      "kpiTrend": "95",
      "kpiTrendState": "1"
    },
    {
      "kpiName": "其它告警响应率",
      "kpiValue": "265.00%",
      "kpiTrend": "55%",
      "kpiTrendState": "1"
    }
  ],
  "total": 1,
  "pageSize": 0,
  "pageCount": 0,
  "success": true
}
```

### 具体40家分行的告警kpi指标

GET http://localhost:8085/infras/workBench/bankKPI?type=day

- 返回值示例
```json
{
  "state": 200,
  "message": "操作成功！",
  "data": [
    {
      "bankId": "0_723",
      "bankName": "北京分行",
      "pueDeviceId": "0_914",
      "pueSpotId": "0_914_1_6019_0",
      "pue": "1.01",
      "rate": "5355.00%",
      "state": "0"
    },
    {
      "bankId": "0_927",
      "bankName": "沈阳分行",
      "pueDeviceId": "0_1610",
      "pueSpotId": "0_1610_1_6019_0",
      "pue": "1.33",
      "rate": "0%",
      "state": "0"
    }
  ],
  "total": 1,
  "pageSize": 0,
  "pageCount": 0,
  "success": true
}
```

### 站点统计(总行主界面-投屏&终端);站点统计:总计,分行,二级分行,支行,村镇银行;总行有单独系统,不在统计之列

GET http://localhost:8085/infras/head/siteStatis

- 返回值示例
```json
{
  "state": 200,
  "message": "操作成功！",
  "data": {
    "total": 48,
    "totalOn": 48,
    "level1Total": 40,
    "level1TotalOn": 40,
    "level2Total": 2,
    "level2TotalOn": 2,
    "level3Total": 4,
    "level3TotalOn": 4,
    "level4Total": 2,
    "level4TotalOn": 2
  },
  "total": 1,
  "pageSize": 0,
  "pageCount": 0,
  "success": true
}
```

### 综合评价

GET http: //localhost:8085/infras/head/evaluates

- 返回值示例
```json
{
  "state": 200,
  "message": "操作成功！",
  "data": [
    {
      "bankName": "贵阳分行",
      "list": [
        {
          "id": 5375,
          "name": "机房设施配置",
          "score": 0.3
        },
        {
          "id": 5404,
          "name": "运行指标",
          "score": 9.0
        },
        {
          "id": 5410,
          "name": "告警处理率",
          "score": 15.0
        },
        {
          "id": 5416,
          "name": "健康评估",
          "score": 10.0
        },
        {
          "id": 5418,
          "name": "标准程序",
          "score": 0.0
        },
        {
          "id": 5423,
          "name": "运维管理",
          "score": 0.0
        }
      ]
    },
    {
      "bankName": "北京分行",
      "list": [
        {
          "id": 5375,
          "name": "机房设施配置",
          "score": 0.0
        },
        {
          "id": 5404,
          "name": "运行指标",
          "score": 0.0
        },
        {
          "id": 5410,
          "name": "告警处理率",
          "score": 15.0
        },
        {
          "id": 5416,
          "name": "健康评估",
          "score": 0.0
        },
        {
          "id": 5418,
          "name": "标准程序",
          "score": 4.0
        },
        {
          "id": 5423,
          "name": "运维管理",
          "score": 0.0
        }
      ]
    }
  ],
  "total": 2,
  "pageSize": 0,
  "pageCount": 0,
  "success": true
}
```