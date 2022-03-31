from django.contrib.auth.models import AbstractUser
from django.db import models
from django.utils import timezone
from django.core.validators import MaxValueValidator, MinValueValidator

class User(AbstractUser):
    pass    

class Listing(models.Model):
    title = models.CharField(max_length = 64)
    description = models.CharField(max_length = 256)
    bid = models.DecimalField(max_digits = 15, decimal_places = 2, validators = [MinValueValidator(1)])
    price = models.DecimalField(max_digits = 15, decimal_places = 2)
    img_url = models.URLField(null = True)
    category = models.CharField(max_length = 64, null = True)
    post_time = models.DateTimeField(default = timezone.now, editable = False)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='creater', null=True)
    closed_time = models.DateTimeField(null=True)
    status = models.BooleanField(default=True)

    def __str__(self):
        return f"{self.id}: {self.title} : {self.price}"

class Bids(models.Model):
    item = models.ForeignKey(Listing, on_delete=models.CASCADE, related_name="listing_bids", null=True)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="bidder", null = True)
    bid = models.DecimalField(max_digits = 15, decimal_places = 2, null = True)

class Comments(models.Model):
    comment = models.CharField(max_length = 256, null=True)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="poster", null = True)
    item = models.ForeignKey(Listing, on_delete=models.CASCADE, related_name="listing_comments", null=True)

class Watchlist(models.Model):
    user = models.ForeignKey(User, null = True, related_name="user_session", on_delete=models.CASCADE)
    item = models.ForeignKey(Listing, on_delete=models.CASCADE, related_name="listing_watchlist", null=True)
