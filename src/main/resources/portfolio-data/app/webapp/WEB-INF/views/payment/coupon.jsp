<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>쿠폰 사용 취소</title></head>
<body>
<h1>쿠폰 사용 취소</h1>
<form method="post" action="/payment/coupon/cancel">
    <label>쿠폰 사용번호 <input name="redemptionNo"></label>
    <button type="submit">사용 취소</button>
</form>
</body>
</html>
