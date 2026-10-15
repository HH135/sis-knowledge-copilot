<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>예약 등록</title></head>
<body>
<h1>예약 등록</h1>
<form method="post" action="/appointment/register">
    <input name="appointmentNo" placeholder="예약번호">
    <input name="customerNo" placeholder="고객번호">
    <input name="vin" placeholder="차량 식별번호">
    <input name="appointmentAt" placeholder="예약 일시">
    <button type="submit">등록</button>
</form>
</body>
</html>
