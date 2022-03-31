from django.contrib import admin
from .models import Listing, Comments, Bids
# Register your models here.
class ListingAdmin(admin.ModelAdmin):
    list_display = ("id", "title", "description", "img_url", "category", "status")

class CommentsAdmin(admin.ModelAdmin):
    list_display = ("id", "user", "comment")

class BidsAdmin(admin.ModelAdmin):
    list_display = ("id", "user", "bid", "item")

admin.site.register(Listing, ListingAdmin)
admin.site.register(Comments)
admin.site.register(Bids)

#superuser Graphiiz pass Graph06042536