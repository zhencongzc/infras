### 银行设备测点信息-查询（从KE工程组态获取）
POST http://localhost:8085/infras/config/bankSpotListFromKeByResourceId/{{resourceId}}
Content-Type: application/json

{
"pageCount": 1,
"pageSize": 10
}

#### 响应体
```json
{
  "state": 200,
  "message": "操作成功！",
  "data": {
    "total_count": 2,
    "resources": [
      {
        "deleted": 0,
        "resource_id": "0_1612_1_1_0",
        "attributes": {
          "value_type": "float",
          "access": "r",
          "converter": "",
          "precision": "2",
          "mapper": "",
          "value_source": 0,
          "privilege": "1",
          "storage": "",
          "event_rules": [
            "event_generator=DefEventGenerator;restore_operand=69;id=163163546184529;content=;suggest=;twinkle_time=;continuous_time=;restore_operator=<;codecex=;operator=>;alarm_type=2;level=1;operand=70;disabled=false"
          ],
          "default": "",
          "ci_type": "3",
          "professional_type": "",
          "id": "1_1_0",
          "group": "1",
          "aggregator": "0",
          "codecex": "",
          "data_source": "0",
          "spot_type": "1",
          "filter": "filter=DefFilter;max=80;min=0;type=num;times=2",
          "codec": "",
          "unit": "kW",
          "input_params": [],
          "parent_id": "0_1612",
          "name": "实时功率",
          "resource_id": "0_1612_1_1_0",
          "location": "project_root/0_722/0_728/0_799/0_1612",
          "order_num": 1,
          "compressor": "compressor=DefCompressor;type=value;param=0.5;interval=60"
        },
        "version": 8903
      },
      {
        "deleted": 0,
        "resource_id": "0_1612_1_2_0",
        "attributes": {
          "value_type": "float",
          "access": "r",
          "converter": "",
          "precision": "1",
          "mapper": "",
          "value_source": 0,
          "privilege": "1",
          "storage": "",
          "event_rules": [
            "disabled=false;operand=60;continuous_time=;restore_operand=59;suggest=;alarm_type=2;restore_operator=<;content=;level=1;id=163163553503646;event_generator=DefEventGenerator;twinkle_time=;operator=>;codecex="
          ],
          "default": "",
          "ci_type": "3",
          "professional_type": "",
          "id": "1_2_0",
          "group": "1",
          "aggregator": "0",
          "codecex": "",
          "data_source": "0",
          "spot_type": "1",
          "filter": "filter=DefFilter;max=100;min=0;type=num;times=2",
          "codec": "",
          "unit": "%",
          "input_params": [],
          "parent_id": "0_1612",
          "name": "负载率",
          "resource_id": "0_1612_1_2_0",
          "location": "project_root/0_722/0_728/0_799/0_1612",
          "order_num": 3,
          "compressor": "compressor=DefCompressor;type=value;param=0.5;interval=60"
        },
        "version": 8903
      }
    ],
    "relations": [],
    "resource_count": 2
  },
  "total": 1,
  "pageSize": 0,
  "pageCount": 0,
  "success": true
}
```

### 设备测点-查询（从KE工程组态获取）
POST http://localhost:8085/infras/config/bankSpotLatest
Content-Type: application/json

{"resources":[{"resource_id":"0_1612_1_1_0"},{"resource_id":"0_1612_1_2_0"}]}


#### 响应体
```json
{
  "state": 200,
  "message": "操作成功！",
  "data": {
    "resources": [
      {
        "status": "1",
        "server_time": "1646378221",
        "resource_id": "0_1612_1_1_0",
        "real_value": "-",
        "event_count": "0=0;1=0;2=0;3=0;4=0;5=0;",
        "alias": "0_1612_1_1_0",
        "save_time": "1646378221"
      },
      {
        "status": "1",
        "server_time": "1646378221",
        "resource_id": "0_1612_1_2_0",
        "real_value": "-",
        "event_count": "0=0;1=0;2=0;3=0;4=0;5=0;",
        "alias": "0_1612_1_2_0",
        "save_time": "1646378221"
      }
    ]
  },
  "total": 1,
  "pageSize": 0,
  "pageCount": 0,
  "success": true
}
```