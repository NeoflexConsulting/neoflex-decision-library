# Approval strategy

[ApprovalStrategyFileIoApp](src/main/scala/ru/neoflex/ndk/strategy/ApprovalStrategyFileIoApp.scala) - пример приложения, которое читает заявку из json-файла, запускает бизнес процесс и записывает результат работы в json-файл.

Файл с заявкой - [application.json](src/main/resources/application.json)
Результаты работы процесса будут находиться в директории [runs](runs): `result.json` - файл с результатом работы стратегии, `flow_trace-*.json` - файл с трассировочной информацией о выполнении стратегии.

[CompareRunsSpec](src/test/scala/ru/neoflex/ndk/strategy/CompareRunsSpec.scala) - пример использования фреймворка для функционального тестирования.
В тесте одна и та же заявка из файла [application.json](src/main/resources/application.json) прогоняется через две разные версии стратегии. Результат работы каждой из стратегий записывается в отдельный json-файл.
Также вместе с результатом в отдельный файл записывается трассировочная информация.
Результаты работы теста будут находиться в директории [compare](runs/compare): `flow_trace-*.json` - файлы с трассировкой конкретной версии стратегии, `result-*.json` - файл с результатом работы конкретной версии стратегии.
Файлы `result_v1.json` и `result_v2.json` можно сравнить и посмотреть разницу в работе версий стратегии.

В трассировчной информации для таблиц, рулов и гейтвеев добавилось поле `details`, в котором содержится дополнителная информация об условиях сработавших веток.

Хардчеки сделаны в виде рулов в отдельных классах-flow с использованием статических функций: [HardChecksFlow](src/main/scala/ru/neoflex/ndk/strategy/flow/HardChecksFlow.scala)
