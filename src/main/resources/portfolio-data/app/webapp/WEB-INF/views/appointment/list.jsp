<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>정비 예약</title></head>
<body>
<h1>정비 예약</h1>
<form method="post" action="/appointment/register">
    <label>예약번호 <input name="appointmentNo"></label>
    <label>고객번호 <input name="customerNo"></label>
    <label>차량 식별번호 <input name="vin"></label>
    <label>예약 일시 <input name="appointmentAt"></label>
    <button type="submit">예약 등록</button>
</form>
<form method="post" action="/appointment/confirm">
    <label>예약번호 <input name="appointmentNo"></label>
    <label>정비 베이번호 <input name="bayNo"></label>
    <button type="submit">예약 확정</button>
</form>
</body>
</html>
