-- Demo katalog — FAQAT dev profilida (application-dev.yml: spring.flyway.locations).
-- Repeatable (R__): versiya raqami asosiy migratsiyalar bilan to'qnashmaydi, ulardan keyin bajariladi
-- va fayl o'zgarsa qayta ishlaydi. Shuning uchun har bir INSERT takrorlanmaydi (WHERE NOT EXISTS).

insert into categories (name, sort_order)
select v.name, v.sort_order
from (values ('Noutbuklar', 1), ('Protsessorlar', 2), ('Videokartalar', 3), ('Xotira (RAM)', 4), ('Serverlar', 5))
         as v(name, sort_order)
where not exists (select 1 from categories c where c.name = v.name);

insert into brands (name, slug, logo, description, country, founded, website)
select v.name, v.slug, v.logo, v.description, v.country, v.founded, v.website
from (values ('Lenovo', 'lenovo', null, 'Noutbuklar, ish stantsiyalari va serverlar.', 'Xitoy', 1984, 'https://www.lenovo.com'),
             ('AMD', 'amd', null, 'Protsessorlar va videokartalar.', 'AQSh', 1969, 'https://www.amd.com'),
             ('NVIDIA', 'nvidia', null, 'Videokartalar va hisoblash tezlatgichlari.', 'AQSh', 1993, 'https://www.nvidia.com'),
             ('Kingston', 'kingston', null, 'Operativ xotira va SSD.', 'AQSh', 1987, 'https://www.kingston.com'),
             ('Dell', 'dell', null, 'Serverlar va ish stantsiyalari.', 'AQSh', 1984, 'https://www.dell.com'))
         as v(name, slug, logo, description, country, founded, website)
where not exists (select 1 from brands b where b.slug = v.slug);

insert into products (sku, name, description, category_id, brand_id, price, specs, status,
                      mxik_code, package_code, vat_percent, created_at, updated_at)
select v.sku, v.name, v.description,
       (select id from categories where name = v.category),
       (select id from brands where slug = v.brand),
       cast(v.price as numeric(14, 2)), v.specs, v.status,
       null, null, cast(12 as numeric(5, 2)), current_timestamp, current_timestamp
from (values
          ('LEN-LEGION-5-PRO', 'Lenovo Legion Pro 5', 'O''yin noutbuki, 16 dyuym.', 'Noutbuklar', 'lenovo', '1499.00',
           'CPU: Ryzen 7 7745HX' || chr(10) || 'RAM: 16GB' || chr(10) || 'GPU: RTX 4060', 'PUBLISHED'),
          ('LEN-IDEAPAD-5', 'Lenovo IdeaPad 5', 'Ish va o''qish uchun.', 'Noutbuklar', 'lenovo', '749.00',
           'CPU: Ryzen 5 7530U' || chr(10) || 'RAM: 16GB', 'PUBLISHED'),
          ('LEN-THINKPAD-E14', 'Lenovo ThinkPad E14', 'Biznes noutbuk.', 'Noutbuklar', 'lenovo', '899.00',
           'CPU: Core i5-1335U' || chr(10) || 'RAM: 16GB', 'DRAFT'),
          ('AMD-R5-5600X', 'AMD Ryzen 5 5600X', '6 yadro, 12 oqim.', 'Protsessorlar', 'amd', '139.99',
           'Yadro: 6' || chr(10) || 'Socket: AM4', 'PUBLISHED'),
          ('AMD-R7-7800X3D', 'AMD Ryzen 7 7800X3D', 'O''yinlar uchun eng tez.', 'Protsessorlar', 'amd', '389.00',
           'Yadro: 8' || chr(10) || 'Socket: AM5', 'PUBLISHED'),
          ('NV-RTX-4060TI', 'NVIDIA GeForce RTX 4060 Ti 8GB', null, 'Videokartalar', 'nvidia', '399.00',
           'Xotira: 8GB GDDR6', 'PUBLISHED'),
          ('NV-RTX-4070S', 'NVIDIA GeForce RTX 4070 Super 12GB', null, 'Videokartalar', 'nvidia', '629.00',
           'Xotira: 12GB GDDR6X', 'PUBLISHED'),
          ('NV-RTX-4080S', 'NVIDIA GeForce RTX 4080 Super 16GB', null, 'Videokartalar', 'nvidia', '1049.00',
           'Xotira: 16GB GDDR6X', 'DRAFT'),
          ('KF-DDR5-32-6000', 'Kingston Fury Beast DDR5 32GB (2x16) 6000', null, 'Xotira (RAM)', 'kingston', '109.00',
           'Hajm: 32GB' || chr(10) || 'Chastota: 6000 MT/s', 'PUBLISHED'),
          ('KF-DDR4-16-3200', 'Kingston Fury Beast DDR4 16GB 3200', null, 'Xotira (RAM)', 'kingston', '42.50',
           'Hajm: 16GB', 'PUBLISHED'),
          ('DELL-R760', 'Dell PowerEdge R760', '2U rack server.', 'Serverlar', 'dell', '8990.00',
           'CPU: 2x Xeon Silver' || chr(10) || 'RAM: 64GB', 'PUBLISHED'),
          ('DELL-T160', 'Dell PowerEdge T160', 'Kichik ofis uchun tower server.', 'Serverlar', 'dell', '1590.00',
           'CPU: Xeon E-2414' || chr(10) || 'RAM: 16GB', 'ARCHIVED'))
         as v(sku, name, description, category, brand, price, specs, status)
where not exists (select 1 from products p where p.sku = v.sku);
