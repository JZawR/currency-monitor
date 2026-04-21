# 🔹 Последнее значение + статистика за 24ч
curl http://localhost:8089/api/history/summary?base=USD&quot=RUB

# 🔹 Детальная история за неделю
curl "http://localhost:8089/api/history/raw?base=USD&quot=RUB&from=2026-04-14T00:00:00&to=2026-04-21T23:59:59"

# 🔹 Агрегация по дням (за месяц)
curl "http://localhost:8089/api/history/daily?base=USD&quot=RUB&from=2026-03-21T00:00:00&to=2026-04-21T23:59:59"

# 🔹 Агрегация по часам (за последние 48ч)
curl "http://localhost:8089/api/history/hourly?base=USD&quot=RUB"


запуск:
docker-compose up -d    

логи:
docker logs -f currency-monitor

график
http://localhost:8089/index.html